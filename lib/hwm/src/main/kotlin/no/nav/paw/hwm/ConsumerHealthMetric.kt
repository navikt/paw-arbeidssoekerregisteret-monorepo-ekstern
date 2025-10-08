package no.nav.paw.hwm

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import java.io.Closeable
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class ConsumerHealthMetric(
    private val registry: PrometheusMeterRegistry,
    private val groupId: String
): OnAssigned, OnRevoked {
    private data class Key(val topic: String, val partition: Int)
    private val assignedPartitions = ConcurrentHashMap<Key, QosGauge>()
    private data class QosGauge(
        val lastPollMeterId: Meter.Id,
        val lastPollTimestamp: AtomicLong,
        val latencyMeterId: Meter.Id,
        val latency: AtomicLong
    )

    private fun createGauge(topic: String, partition: Int): QosGauge {
        val lastPollTimestamp = AtomicLong(0L)
        val latency = AtomicLong(0L)
        val lastPollTimestampGauge = Gauge.builder(
            "last_consumer_poll_timestamp",
            lastPollTimestamp,
            { it.get().toDouble() }
        ).tags(
            Tags.of(
            Tag.of("topic", topic),
            Tag.of("partition", partition.toString()),
            Tag.of("group_id", groupId),
        )).register(registry)
        val latencyGauge = Gauge.builder(
            "processed_record_latency",
            latency,
            { it.get().toDouble() }
        ).tags(Tags.of(
            Tag.of("topic", topic),
            Tag.of("partition", partition.toString()),
            Tag.of("group_id", groupId),
        )).register(registry)
        return QosGauge(
            lastPollMeterId = lastPollTimestampGauge.id,
            lastPollTimestamp = lastPollTimestamp,
            latencyMeterId = latencyGauge.id,
            latency = latency
        )
    }

    override fun assigned(topic: String, partition: Int) {
        assignedPartitions.compute(Key(topic, partition)) { key, current ->
            current ?: createGauge(key.topic, key.partition)
        }
    }

    override fun revoked(topic: String, partition: Int) {
        assignedPartitions.remove(Key(topic, partition))
            ?.also { qosGauge ->
                registry.remove(qosGauge.latencyMeterId)
                registry.remove(qosGauge.lastPollMeterId)
            }
    }

    fun consumerPollProcessed(timestamp: Instant) {
        assignedPartitions.values.forEach { (_, kilde) ->
            kilde.set(timestamp.toEpochMilli())
        }
    }

    fun recordProcessed(topic: String, partisjon: Int, recordTimestampMs: Long) {
        assignedPartitions[Key(topic, partisjon)]
            ?.latency
            ?.set(System.currentTimeMillis() - recordTimestampMs)
    }

}