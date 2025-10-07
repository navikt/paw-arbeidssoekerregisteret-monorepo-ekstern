package no.nav.paw.hwm

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.health.LivenessCheck
import no.nav.paw.health.StartupCheck
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Duration
import java.time.Duration.between
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.min

fun <K, V> Consumer<K, V>.withHwmAndMetrics(
    prometheusMeterRegistry: PrometheusMeterRegistry,
    receiver: (Sequence<ConsumerRecord<K, V>>) -> Unit,
    pollTimeout: Duration = Duration.ofMillis(1000L),
    isAliveTimeout: Duration = Duration.ofSeconds(10),
    hwmTopicConfig: Iterable<HwmTopicConfig>
): DataConsumer<ConsumerRecord<K, V>, K, V> =
    DataConsumer(
        consumer = this,
        prometheusMeterRegistry = prometheusMeterRegistry,
        converter = { it },
        receiver = receiver,
        pollTimeout = pollTimeout,
        isAliveTimeout = isAliveTimeout,
        hwmTopicConfig = hwmTopicConfig
    )

fun <K, V> Consumer<K, V>.asMessageConsumerWithHwmAndMetrics(
    prometheusMeterRegistry: PrometheusMeterRegistry,
    receiver: (Sequence<Message<K, V>>) -> Unit,
    pollTimeout: Duration = Duration.ofMillis(1000L),
    isAliveTimeout: Duration = Duration.ofSeconds(10),
    hwmTopicConfig: Iterable<HwmTopicConfig>
): DataConsumer<Message<K, V>, K, V> =
    DataConsumer(
        consumer = this,
        prometheusMeterRegistry = prometheusMeterRegistry,
        pollTimeout = pollTimeout,
        isAliveTimeout = isAliveTimeout,
        hwmTopicConfig = hwmTopicConfig,
        converter = { it.toMessage() },
        receiver = receiver
    )


class DataConsumer<B, K, V> internal constructor(
    private val consumer: Consumer<K, V>,
    prometheusMeterRegistry: PrometheusMeterRegistry,
    private val converter: (ConsumerRecord<K, V>) -> B,
    private val receiver: (Sequence<B>) -> Unit,
    private val pollTimeout: Duration = Duration.ofMillis(1000L),
    private val isAliveTimeout: Duration = Duration.ofSeconds(10),
    hwmTopicConfig: Iterable<HwmTopicConfig>
) : LivenessCheck, StartupCheck {
    private val consumerHealthMetric = ConsumerHealthMetric(prometheusMeterRegistry, consumer.groupMetadata().groupId())
    private val lastPollProcessingCompletedAt = AtomicReference(Instant.EPOCH)
    private val hasStarted = AtomicBoolean(false)
    private val topicToConsumerVersion: Map<String, Int> = hwmTopicConfig.associate { it.topic to it.consumerVersion }

    private fun consumerVersion(topic: String): Int =
        topicToConsumerVersion[topic]
            ?: throw IllegalStateException("Mottok melding fra topic: $topic som ikke er konfigurert i HwmTopicConfig")

    override fun hasStarted(): Boolean {
        return lastPollProcessingCompletedAt.get() > Instant.EPOCH
    }

    override fun isAlive(): Boolean {
        return between(lastPollProcessingCompletedAt.get(), Instant.now()) < isAliveTimeout
    }

    fun run(): CompletableFuture<Void> {
        if (!hasStarted.compareAndSet(false, true)) {
            throw IllegalStateException("DataConsumer kan kun startes en gang")
        }
        return CompletableFuture.runAsync {
            consumer.use { consumer ->
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
                                            consumerVersion = consumerVersion(record.topic()),
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
                                    .map { record -> converter(record) }
                                    .let(receiver)
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
                    lastPollProcessingCompletedAt.set(now)
                    consumerHealthMetric.consumerPollProcessed(now)
                }
            }
        }
    }
}
