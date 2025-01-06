package no.nav.paw.arbeidssoekerregisteret.eksternt.api.kafka

import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.services.PeriodeService
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils.buildLogger
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration

class PeriodeConsumer(
    private val topic: String,
    private val consumer: KafkaConsumer<Long, Periode>,
    private val periodeService: PeriodeService
) {
    private val logger = buildLogger
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
        val records = consumer.poll(Duration.ofMillis(1000))
        processAndCommitBatch(records)
    }

    private fun processAndCommitBatch(records: ConsumerRecords<Long, Periode>) =
        try {
            periodeService.handleRecords(records)
            consumer.commitSync()
        } catch (error: Exception) {
            throw Exception("Feil ved konsumering av melding fra $topic", error)
        }
}
