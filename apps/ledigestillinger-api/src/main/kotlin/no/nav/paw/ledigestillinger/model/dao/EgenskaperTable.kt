package no.nav.paw.ledigestillinger.model.dao

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.selectAll

object EgenskaperTable : LongIdTable("egenskaper") {
    val parentId = long("parent_id").references(StillingerTable.id)
    val key = varchar("key", 50)
    val value = text("value")
}

fun EgenskaperTable.selectRowsByParentId(
    parentId: Long
): List<EgenskapRow> = selectAll()
    .where { EgenskaperTable.parentId eq parentId }
    .map { it.asEgenskapRow() }

fun EgenskaperTable.insert(
    parentId: Long,
    rows: Iterable<EgenskapRow>
) = batchInsert(rows) { row ->
    this[EgenskaperTable.parentId] = parentId
    this[EgenskaperTable.key] = row.key
    this[EgenskaperTable.value] = row.value
}
