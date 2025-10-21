package no.nav.paw.ledigestillinger.model.dao

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

object KlassifiseringerTable : LongIdTable("klassifiseringer") {
    val parentId = long("parent_id").references(StillingerTable.id)
    val type = varchar("type", 255)
    val kode = varchar("kode", 255)
    val navn = varchar("navn", 255)
}

fun KlassifiseringerTable.selectRowsByParentId(
    parentId: Long
): List<KlassifiseringRow> = selectAll()
    .where { KlassifiseringerTable.parentId eq parentId }
    .map { it.asKlassifiseringRow() }

fun KlassifiseringerTable.insert(
    parentId: Long,
    row: KlassifiseringRow
): Long = insertAndGetId {
    it[this.parentId] = parentId
    it[this.type] = row.type
    it[this.kode] = row.kode
    it[this.navn] = row.navn
}.value

fun KlassifiseringerTable.updateByParentId(
    parentId: Long,
    row: KlassifiseringRow
): Int = update(
    where = { KlassifiseringerTable.parentId eq parentId }
) {
    it[this.parentId] = parentId
    it[this.type] = row.type
    it[this.kode] = row.kode
    it[this.navn] = row.navn
}