package no.nav.paw.ledigestillinger.model.dao

import no.nav.paw.ledigestillinger.model.asEgenskapRow
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.selectAll

object EgenskaperTable : LongIdTable("egenskaper_v2") {
    val parentId = long("parent_id").references(StillingerTable.id)
    val key = varchar("key", 50)
    val value = text("value")

    fun selectRowsByParentId(
        parentId: Long
    ): List<EgenskapRow> = selectAll()
        .where { EgenskaperTable.parentId eq parentId }
        .map { it.asEgenskapRow() }

    fun insert(
        parentId: Long,
        rows: Iterable<EgenskapRow>
    ) = batchInsert(rows) { row ->
        this[EgenskaperTable.parentId] = parentId
        this[key] = row.key
        this[value] = row.value
    }
}
