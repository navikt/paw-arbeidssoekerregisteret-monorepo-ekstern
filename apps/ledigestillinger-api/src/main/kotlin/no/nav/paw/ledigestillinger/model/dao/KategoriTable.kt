package no.nav.paw.ledigestillinger.model.dao

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.selectAll

object KategorierTable : LongIdTable("kategorier") {
    val parentId = long("parent_id").references(StillingerTable.id)
    val kode = varchar("kode", 255)
    val normalisertKode = varchar("normalisert_kode", 255)
    val navn = varchar("navn", 255)
}

fun KategorierTable.selectRowsByParentId(
    parentId: Long
): List<KategoriRow> = selectAll()
    .where { KategorierTable.parentId eq parentId }
    .map { it.asKategoriRow() }

fun KategorierTable.insert(
    parentId: Long,
    rows: Iterable<KategoriRow>
) = batchInsert(rows) { row ->
    this[KategorierTable.parentId] = parentId
    this[KategorierTable.kode] = row.kode
    this[KategorierTable.normalisertKode] = row.normalisertKode
    this[KategorierTable.navn] = row.navn
}
