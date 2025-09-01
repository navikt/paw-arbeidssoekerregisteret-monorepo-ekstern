package no.nav.paw.oppslagapi.data.consumer

import io.opentelemetry.api.trace.Span
import no.nav.paw.oppslagapi.appLogger
import no.nav.paw.oppslagapi.consumer_version
import no.nav.paw.oppslagapi.data.consumer.kafka.hwm.updateHwm
import no.nav.paw.oppslagapi.health.HasStarted
import no.nav.paw.oppslagapi.health.IsAlive
import no.nav.paw.oppslagapi.health.Status
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.common.serialization.Deserializer
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration
import java.time.Duration.between
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.min

class DataConsumer(
    private val deserializer: Deserializer<SpecificRecord>,
    private val consumer: Consumer<Long, ByteArray>,
    private val pollTimeout: Duration = Duration.ofMillis(1000L),
    private val consumerHealthMetric: ConsumerHealthMetric
) : IsAlive, HasStarted {
    override val name = "DataConsumer(consumer_version=$consumer_version, pollTimeout=$pollTimeout)"

    private val sisteProessering = AtomicReference(Instant.EPOCH)
    private val erStartet = AtomicBoolean(false)
    private val exitStatus = AtomicReference<Status.ERROR?>(null)

    override fun hasStarted(): Status {
        val currentExitError = exitStatus.get()
        return when {
            currentExitError != null -> currentExitError
            sisteProessering.get() > Instant.EPOCH -> Status.OK
            else -> Status.PENDING("Ikke startet")
        }
    }

    override fun isAlive(): Status {
        return exitStatus.get() ?: between(sisteProessering.get(), Instant.now())
            .let { timeSinceLastCompletedPoll ->
                if (timeSinceLastCompletedPoll < Duration.ofSeconds(10)) {
                    Status.OK
                } else {
                    Status.PENDING("${timeSinceLastCompletedPoll.toMillis()} ms siden siste batch ble prosessert")
                }
            }
    }

    fun run(): CompletableFuture<Void> {
        if (!erStartet.compareAndSet(false, true)) {
            throw IllegalStateException("DataConsumer kan kun startes en gang")
        }
        return CompletableFuture.runAsync {
            appLogger.info("Startet DataConsumer for consumer_version=${consumer_version}")
            runCatching {
                while (true) {
                    consumer.poll(pollTimeout)
                        .takeIf { !it.isEmpty }
                        ?.let { records ->
                            val eldsteRecordTidspunkt = mutableMapOf<Pair<String, Int>, Long>()
                            transaction {
                                records
                                    .asSequence()
                                    .filter { record ->
                                        updateHwm(
                                            consumerVersion = consumer_version,
                                            topic = record.topic(),
                                            partition = record.partition(),
                                            offset = record.offset()
                                        )
                                    }
                                    .onEach { record ->
                                        eldsteRecordTidspunkt.compute(record.topic() to record.partition()) { _, current ->
                                            current?.let { min(it, record.timestamp()) } ?: record.timestamp()
                                        }
                                    }
                                    .map { record -> record.toRow(deserializer) to Span.current() }
                                    .let(::writeBatchToDb)
                            }
                            eldsteRecordTidspunkt.forEach { (key, value) ->
                                consumerHealthMetric.recordProcessed(
                                    topic = key.first,
                                    partisjon = key.second,
                                    recordTimestampMs = value
                                )
                            }
                        }
                    val now = Instant.now()
                    sisteProessering.set(now)
                    consumerHealthMetric.consumerPollProcessed(now)
                }
            }.onFailure { throwable ->
                runCatching { consumer.close(Duration.ofSeconds(1)) }
                appLogger.error("DataConsumer avsluttet med feil", throwable)
                exitStatus.set(Status.ERROR(message = "Stoppet grunnet feil", cause = throwable))
            }.onSuccess {
                runCatching { consumer.close(Duration.ofSeconds(1)) }
                appLogger.info("DataConsumer avsluttet uten feil")
            }
        }
    }
}

