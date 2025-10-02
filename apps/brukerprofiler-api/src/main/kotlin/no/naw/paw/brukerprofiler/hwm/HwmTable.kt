package no.naw.paw.brukerprofiler.hwm

import org.jetbrains.exposed.sql.Table

object HwmTable: Table("hwm") {
    val version = integer("version")
    val topic = varchar("kafka_topic", 255)
    val partition = integer("kafka_partition")
    val offset = long("kafka_offset")
    override val primaryKey: PrimaryKey = PrimaryKey(version, topic, partition)
}