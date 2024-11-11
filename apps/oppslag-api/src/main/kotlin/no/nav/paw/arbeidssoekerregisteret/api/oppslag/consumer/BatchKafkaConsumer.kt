package no.nav.paw.arbeidssoekerregisteret.api.oppslag.consumer

import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.SpanAttribute
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.buildLogger
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration

class BatchKafkaConsumer<K, V>(
    private val topic: String,
    val consumer: KafkaConsumer<K, V>,
    private val receiver: (Sequence<V>) -> Unit
) {
    private val pollingInterval = Duration.ofMillis(100)

    fun subscribe() {
        consumer.subscribe(listOf(topic))
    }

    fun stop() {
        consumer.unsubscribe()
        consumer.close()
    }

    @WithSpan(
        value = "get_and_process_batch",
        kind = SpanKind.CONSUMER
    )
    fun getAndProcessBatch(
        @SpanAttribute("topics") topic: String = this.topic
    ) {
        val records = consumer.poll(pollingInterval)
        if (records.isEmpty) {
            buildLogger.trace("Mottok ingen meldinger i intervall")
        } else {
            records.asSequence()
                .onEach {
                    buildLogger.trace(
                        "Mottok melding fra {} med offset {} partition {}",
                        it.topic(),
                        it.offset(),
                        it.partition()
                    )
                }.map { it.value() }
                .runCatching(receiver)
                .mapCatching { commitSync() }
                .fold(
                    onSuccess = { buildLogger.debug("Batch behandlet og committet") },
                    onFailure = { error -> throw Exception("Feil ved konsumering av melding fra $topic", error) }
                )
        }
    }

    @WithSpan(
        value = "commit_sync",
        kind = SpanKind.CLIENT
    )
    private fun commitSync() {
        consumer.commitSync()
    }
}
