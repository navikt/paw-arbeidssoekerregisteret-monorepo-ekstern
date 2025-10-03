package no.nav.paw.arbeidssoekerregisteret

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.hwm.OnAssigned
import no.nav.paw.hwm.OnRevoked
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class ConsumerHealthMetric(
    private val registry: PrometheusMeterRegistry,
): OnAssigned, OnRevoked {
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

    override fun assigned(topic: String, partition: Int) {
        tildeltePartisjoner.compute(Key(topic, partition)) { key, current ->
            current ?: createGauge(key.topic, key.partisjon)
        }
    }

    override fun revoked(topic: String, partition: Int) {
        tildeltePartisjoner.remove(Key(topic, partition))
            ?.also { qosGauge ->
                registry.remove(qosGauge.forsinkelseMeterId)
                registry.remove(qosGauge.sistePollMeterId)
            }
    }
}