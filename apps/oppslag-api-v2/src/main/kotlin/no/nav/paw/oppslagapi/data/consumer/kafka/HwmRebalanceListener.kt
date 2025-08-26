package no.nav.paw.oppslagapi.data.consumer.kafka

import io.opentelemetry.api.common.AttributeKey.longKey
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode.ERROR
import io.opentelemetry.api.trace.StatusCode.OK
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.oppslagapi.data.consumer.kafka.hwm.getHwm
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener
import org.apache.kafka.common.TopicPartition
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

class HwmRebalanceListener(
    private val consumerVersion: Int,
    private val consumer: Consumer<*, *>
) : ConsumerRebalanceListener {

    private val logger = LoggerFactory.getLogger(HwmRebalanceListener::class.java)

    @WithSpan(
        value = "on_partitions_revoked",
        kind = SpanKind.INTERNAL
    )
    override fun onPartitionsRevoked(partitions: MutableCollection<TopicPartition>?) {
        logger.info("Revoked: $partitions")
    }

    @WithSpan(
        value = "on_partitions_assigned",
        kind = SpanKind.INTERNAL
    )
    override fun onPartitionsAssigned(partitions: MutableCollection<TopicPartition>?) {
        runCatching {
            val assignedPartitions = partitions ?: emptyList()
            logger.info("Assigned partitions $assignedPartitions")
            if (assignedPartitions.isNotEmpty()) {
                transaction {
                    assignedPartitions.map { partition ->
                        val offset = requireNotNull(
                            getHwm(
                                consumerVersion = consumerVersion,
                                topic = partition.topic(),
                                partition = partition.partition()
                            )
                        ) {
                            "No hwm for topic:partition ${partition.topic()}:${partition.partition()}, init not called?"
                        }
                        partition to offset
                    }
                }.forEach { (partition, offset) ->
                    val seekTo = offset + 1
                    Span.current().addEvent(
                        "seeking_to_offset",
                        Attributes.of(
                            stringKey("topic"), partition.topic(),
                            longKey("partition"), partition.partition().toLong(),
                            longKey("offset"), seekTo
                        )
                    )
                    consumer.seek(partition, seekTo)
                }
            }
        }
            .onSuccess { Span.current().setStatus(OK) }
            .onFailure { t ->
            Span.current().recordException(t)
            Span.current().setStatus(ERROR, "Failed to set offsets on rebalance")
            Span.current().setAttribute(stringKey("error.type"), t::class.java.canonicalName)
        }.getOrThrow()
    }
}