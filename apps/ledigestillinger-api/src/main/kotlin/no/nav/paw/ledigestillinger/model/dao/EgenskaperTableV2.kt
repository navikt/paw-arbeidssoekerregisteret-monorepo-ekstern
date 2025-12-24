package no.nav.paw.ledigestillinger.model.dao

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.selectAll

object EgenskaperTableV2 : LongIdTable("egenskaper_v2") {
    val parentId = long("parent_id").references(StillingerTableV2.id)
    val key = varchar("key", 50)
    val value = text("value")

    fun selectRowsByParentId(
        parentId: Long
    ): List<EgenskapRow> = selectAll()
        .where { EgenskaperTableV2.parentId eq parentId }
        .map { it.asEgenskapRowV2() }

    fun insert(
        parentId: Long,
        rows: Iterable<EgenskapRow>
    ) = batchInsert(rows) { row ->
        this[EgenskaperTableV2.parentId] = parentId
        this[key] = row.key
        this[value] = row.value
    }
}
