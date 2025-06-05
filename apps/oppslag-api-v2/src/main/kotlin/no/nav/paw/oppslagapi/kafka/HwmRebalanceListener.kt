package no.nav.paw.oppslagapi.kafka

import no.nav.paw.oppslagapi.kafka.hwm.getHwm
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

    override fun onPartitionsRevoked(partitions: MutableCollection<TopicPartition>?) {
        logger.info("Revoked: $partitions")
    }

    override fun onPartitionsAssigned(partitions: MutableCollection<TopicPartition>?) {
        val assignedPartitions = partitions ?: emptyList()
        logger.info("Assigned partitions $assignedPartitions")
        if (assignedPartitions.isNotEmpty()) {
            transaction {
                assignedPartitions.map { partition ->
                    val offset = requireNotNull(getHwm(
                        consumerVersion = consumerVersion,
                        topic = partition.topic(),
                        partition = partition.partition()
                    )) {
                        "No hwm for topic:partition ${partition.topic()}:${partition.partition()}, init not called?"
                    }
                    partition to offset
                }
            }.forEach { (partition, offset) ->
                consumer.seek(partition, offset + 1)
            }
        }
    }
}