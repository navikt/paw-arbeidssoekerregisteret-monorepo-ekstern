package no.nav.paw.oppslagapi.data.consumer

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import java.lang.System.currentTimeMillis
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class ConsumerHealthMetric(
    private val registry: PrometheusMeterRegistry
) {
    private val tildeltePartisjoner = ConcurrentHashMap<Key, QosGauge>()
    private data class Key(val topic: String, val partisjon: Int)
    private data class QosGauge(
        val sistePollMeterId: Meter.Id,
        val sistePollTimestamp: AtomicLong,
        val forsinkelseMeterId: Meter.Id,
        val forsinkelse: AtomicLong
    )

    private fun createGauge(topic: String, partisjon: Int): QosGauge {
        val sistePollTimestamp = AtomicLong(0L)
        val latency = AtomicLong(0L)
        val sistePollTimestampGauge = Gauge.builder(
            "paw_oppslagsapi_last_consumer_poll_timestamp",
            sistePollTimestamp,
            { it.get().toDouble() }
        ).tags(Tags.of(
            Tag.of("topic", topic),
            Tag.of("partition", partisjon.toString())
        )).register(registry)
        val nyesteMeldingTimestampGauge = Gauge.builder(
            "paw_oppslagsapi_processed_record_latency",
            latency,
            { it.get().toDouble() }
        ).tags(Tags.of(
            Tag.of("topic", topic),
            Tag.of("partition", partisjon.toString())
        )).register(registry)
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
        tildeltePartisjoner.compute(Key(topic, partisjon)) { key, current ->
            current?.let { (meterId, _) ->
                registry.remove(meterId)
                null
            }
        }
    }

    fun consumerPollProcessed(timestamp: Instant) {
        tildeltePartisjoner.values.forEach { (_, kilde) ->
            kilde.set(timestamp.toEpochMilli())
        }
    }

    fun recordProcessed(topic: String, partisjon: Int, recordTimestampMs: Long) {
        tildeltePartisjoner[Key(topic, partisjon)]
            ?.forsinkelse
            ?.set(currentTimeMillis() - recordTimestampMs)
    }
}