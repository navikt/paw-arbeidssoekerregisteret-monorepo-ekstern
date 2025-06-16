package no.nav.paw.oppslagapi.dataconsumer

import io.opentelemetry.api.trace.Span
import no.nav.paw.oppslagapi.appLogger
import no.nav.paw.oppslagapi.consumer_version
import no.nav.paw.oppslagapi.dataconsumer.kafka.hwm.updateHwm
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.common.serialization.Deserializer
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class DataConsumer(
    private val deserializer: Deserializer<SpecificRecord>,
    private val consumer: Consumer<Long, ByteArray>,
    private val pollTimeout: Duration = Duration.ofMillis(1000L)
) {
    private val _sisteProessering = AtomicReference<Instant?>(null)
    val sisteProessering: Instant? get() = _sisteProessering.get()

    private val erStartet = AtomicBoolean(false)

    fun run(): CompletableFuture<Void> {
        if (!erStartet.compareAndSet(false, true)) {
            throw IllegalStateException("DataConsumer kan kun startes en gang")
        }
        return CompletableFuture.runAsync {
            appLogger.info("Startet DataConsumer for consumer_version=${consumer_version}")
            try {
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
                            _sisteProessering.set(Instant.now())
                        }
                }
            } catch (_: InterruptedException) {
                appLogger.info("Consumer interrupted, shutting down gracefully")
            } finally {
                runCatching { consumer.close(pollTimeout) }
            }
        }
    }
}

