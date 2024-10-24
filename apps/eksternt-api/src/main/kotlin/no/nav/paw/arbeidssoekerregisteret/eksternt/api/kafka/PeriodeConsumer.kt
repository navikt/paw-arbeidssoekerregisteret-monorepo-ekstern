package no.nav.paw.arbeidssoekerregisteret.eksternt.api.kafka

import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.services.ArbeidssoekerService
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils.logger
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration

class PeriodeConsumer(
    private val topic: String,
    private val consumer: KafkaConsumer<Long, Periode>,
    private val arbeidssoekerService: ArbeidssoekerService
) {
    private var running = true

    fun start() {
        logger.info("Lytter på topic $topic")
        consumer.subscribe(listOf(topic))

        while (running) {
            pollAndProcess()
        }
    }

    fun stop() {
        running = false
        consumer.unsubscribe()
    }

    @WithSpan(
        value = "get_and_process_batch",
        kind = SpanKind.CONSUMER
    )
    private fun pollAndProcess() {
        val perioder =
            consumer.poll(Duration.ofMillis(1000))
                .asSequence()
                .onEach {
                    logger.info("Mottok melding fra $topic med offset ${it.offset()} partition ${it.partition()}")
                }.map(ConsumerRecord<Long, Periode>::value)
        processAndCommitBatch(perioder)
    }

    private fun processAndCommitBatch(batch: Sequence<Periode>) =
        try {
            arbeidssoekerService.storeBatch(batch)
            consumer.commitSync()
        } catch (error: Exception) {
            throw Exception("Feil ved konsumering av melding fra $topic", error)
        }
}
