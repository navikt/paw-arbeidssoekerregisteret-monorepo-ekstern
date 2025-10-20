package no.nav.paw.ledigestillinger.model.dao

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

object KategorierTable : LongIdTable("kategorier") {
    val parentId = long("parent_id").references(StillingerTable.id)
    val kode = varchar("kode", 20)
    val navn = varchar("navn", 255)
}

fun KategorierTable.selectRowsByParentId(
    parentId: Long
): List<KategoriRow> = selectAll()
    .where { KategorierTable.parentId eq parentId }
    .map { it.asKategoriRow() }

fun KategorierTable.insert(
    parentId: Long,
    row: KategoriRow
): Long = insertAndGetId {
    it[this.parentId] = parentId
    it[this.kode] = row.kode
    it[this.navn] = row.navn
}.value

fun KategorierTable.updateByParentId(
    parentId: Long,
    row: KategoriRow
): Int = update(
    where = { KategorierTable.parentId eq parentId }
) {
    it[this.parentId] = parentId
    it[this.kode] = row.kode
    it[this.navn] = row.navn
}