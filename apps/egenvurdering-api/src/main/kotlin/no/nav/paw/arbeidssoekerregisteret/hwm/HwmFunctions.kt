package no.nav.paw.arbeidssoekerregisteret.hwm

import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

fun initHwm(topic: String, consumerVersion: Int, partitionCount: Int) =
    (0 until partitionCount)
        .filter { partition ->
            getHwm(
                consumerVersion = consumerVersion,
                topic = topic,
                partition = partition
            ) == null }
        .forEach { partition ->
            insertHwm(
                consumerVersion = consumerVersion,
                topic = topic,
                partition = partition,
                offset = -1L
            )
        }

fun getHwm(consumerVersion: Int, topic: String, partition: Int): Long? =
    HwmTable
        .selectAll()
        .where {
            (HwmTable.topic eq topic) and
                    (HwmTable.partition eq partition) and
                    (HwmTable.version eq consumerVersion) }
        .singleOrNull()?.get(HwmTable.offset)

fun insertHwm(consumerVersion: Int, topic: String, partition: Int, offset: Long) {
    HwmTable.insert {
        it[HwmTable.version] = consumerVersion
        it[HwmTable.topic] = topic
        it[HwmTable.partition] = partition
        it[HwmTable.offset] = offset
    }
}

fun updateHwm(consumerVersion: Int, topic: String, partition: Int, offset: Long): Boolean =
    HwmTable
        .update({
            (HwmTable.topic eq topic) and
                    (HwmTable.partition eq partition) and
                    (HwmTable.offset less offset) and
                    (HwmTable.version eq consumerVersion)
        }) { it[HwmTable.offset] = offset } == 1
