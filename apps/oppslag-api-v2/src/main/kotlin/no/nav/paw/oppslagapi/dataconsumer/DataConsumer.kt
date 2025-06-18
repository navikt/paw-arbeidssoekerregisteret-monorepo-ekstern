package no.nav.paw.oppslagapi.dataconsumer

import io.opentelemetry.api.trace.Span
import no.nav.paw.oppslagapi.appLogger
import no.nav.paw.oppslagapi.consumer_version
import no.nav.paw.oppslagapi.dataconsumer.kafka.hwm.updateHwm
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

class DataConsumer(
    private val deserializer: Deserializer<SpecificRecord>,
    private val consumer: Consumer<Long, ByteArray>,
    private val pollTimeout: Duration = Duration.ofMillis(1000L)
): IsAlive, HasStarted {
    override val name = "DataConsumer(consumer_version=$consumer_version, pollTimeout=$pollTimeout)"

    private val sisteProessering = AtomicReference(Instant.EPOCH)
    private val erStartet = AtomicBoolean(false)
    private val exitError = AtomicReference<Throwable?>(null)

    override fun hasStarted(): Status {
        val currentExitError = exitError.get()
        return when {
            currentExitError != null -> {
                Status.ERROR("Fatal error", currentExitError)
            }
            sisteProessering.get() > Instant.EPOCH -> {
                Status.OK
            }
            else -> {
                Status.PENDING("DataConsumer has not started yet, last processing time is $sisteProessering")
            }
        }
    }

    override fun isAlive(): Status {
        val currentExitError = exitError.get()
        return if (currentExitError != null) {
            Status.ERROR("Fatal error", currentExitError)
        } else {
            between(sisteProessering.get(), Instant.now())
                .let { timeSinceLastCompletedPoll ->
                    if (timeSinceLastCompletedPoll < Duration.ofSeconds(10)) {
                        Status.OK
                    } else {
                        Status.PENDING("DataConsumer has not processed data in the last ${timeSinceLastCompletedPoll.toMillis()} ms")
                    }
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
                                    .onEach { Span.current().addEvent("added_to_batch") }
                                    .map { record -> record.toRow(deserializer) to Span.current() }
                                    .let(::writeBatchToDb)
                            }
                        }
                    sisteProessering.set(Instant.now())
                }
            }.onFailure { throwable -> exitError.set(throwable) }
        }
    }
}

