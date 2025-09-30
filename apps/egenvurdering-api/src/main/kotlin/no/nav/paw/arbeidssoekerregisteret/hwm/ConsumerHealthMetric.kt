package no.nav.paw.arbeidssoekerregisteret.hwm

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class ConsumerHealthMetric(
    private val registry: PrometheusMeterRegistry,
) {
    private val tildeltePartisjoner = ConcurrentHashMap<Key, QosGauge>()

    private data class Key(val topic: String, val partisjon: Int)
    private data class QosGauge(
        val sistePollMeterId: Meter.Id,
        val sistePollTimestamp: AtomicLong,
        val forsinkelseMeterId: Meter.Id,
        val forsinkelse: AtomicLong,
    )

    private fun createGauge(topic: String, partisjon: Int): QosGauge {
        val sistePollTimestamp = AtomicLong(0L)
        val latency = AtomicLong(0L)
        val sistePollTimestampGauge = Gauge.builder(
            "paw_egenvurdering_api_last_consumer_poll_timestamp",
            sistePollTimestamp,
            { it.get().toDouble() }
        ).tags(
            Tags.of(
                Tag.of("topic", topic),
                Tag.of("partition", partisjon.toString())
            )
        ).register(registry)
        val nyesteMeldingTimestampGauge = Gauge.builder(
            "paw_egenvurdering_api_processed_record_latency",
            latency,
            { it.get().toDouble() }
        ).tags(
            Tags.of(
                Tag.of("topic", topic),
                Tag.of("partition", partisjon.toString())
            )
        ).register(registry)
        return QosGauge(
            sistePollMeterId = sistePollTimestampGauge.id,
            sistePollTimestamp = sistePollTimestamp,
            forsinkelseMeterId = nyesteMeldingTimestampGauge.id,
            forsinkelse = latency
        )
    }

    fun nyTildeling(topic: String, partisjon: Int) {
        tildeltePartisjoner.compute(Key(topic, partisjon)) { key, current ->
            current ?: createGauge(key.topic, key.partisjon)
        }
    }

    fun fjernTildeling(topic: String, partisjon: Int) {
        tildeltePartisjoner.remove(Key(topic, partisjon))
            ?.also { qosGauge ->
                registry.remove(qosGauge.forsinkelseMeterId)
                registry.remove(qosGauge.sistePollMeterId)
            }
    }
}