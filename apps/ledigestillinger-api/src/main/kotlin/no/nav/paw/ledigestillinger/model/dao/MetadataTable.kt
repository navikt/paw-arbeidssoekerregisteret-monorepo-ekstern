package no.nav.paw.ledigestillinger.model.dao

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.javatime.timestamp

object MetadataTable : LongIdTable("metadata") {
    val parentId = long("parent_id").references(StillingerTable.id)
    val status = varchar("status", 30)
    val recordTimestamp = timestamp("record_timestamp")
    val insertedTimestamp = timestamp("inserted_timestamp")
    val updatedTimestamp = timestamp("updated_timestamp").nullable()
}