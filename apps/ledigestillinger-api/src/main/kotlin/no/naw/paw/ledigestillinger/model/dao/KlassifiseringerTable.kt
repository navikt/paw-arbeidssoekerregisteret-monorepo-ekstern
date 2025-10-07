package no.naw.paw.ledigestillinger.model.dao

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object KlassifiseringerTable : LongIdTable("klassifiseringer") {
    val parentId = long("parent_id").references(StillingerTable.id)
    val type = varchar("type", 20)
    val kode = varchar("kode", 20)
    val navn = varchar("navn", 255)
}