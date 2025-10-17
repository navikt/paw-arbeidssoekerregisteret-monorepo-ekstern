package no.nav.paw.ledigestillinger.model.dao

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.update

object EgenskaperTable : LongIdTable("egenskaper") {
    val parentId = long("parent_id").references(StillingerTable.id)
    val key = varchar("key", 50)
    val value = varchar("value", 255)
}

fun EgenskaperTable.insert(
    parentId: Long,
    row: EgenskapRow
): Long = insertAndGetId {
    it[this.parentId] = parentId
    it[this.key] = row.key
    it[this.value] = row.value
}.value

fun EgenskaperTable.updateByParentId(
    parentId: Long,
    row: EgenskapRow
): Int = update(
    where = { EgenskaperTable.parentId eq parentId }
) {
    it[this.key] = row.key
    it[this.value] = row.value
}