package no.nav.paw.ledigestillinger.model.dao

import no.nav.paw.ledigestillinger.model.asKategoriRow
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.selectAll

object KategorierTable : LongIdTable("kategorier_v2") {
    val parentId = long("parent_id").references(StillingerTable.id)
    val kode = varchar("kode", 20)
    val normalisertKode = varchar("normalisert_kode", 20)
    val navn = varchar("navn", 255)

    fun selectRowsByParentId(
        parentId: Long
    ): List<KategoriRow> = selectAll()
        .where { KategorierTable.parentId eq parentId }
        .map { it.asKategoriRow() }

    fun insert(
        parentId: Long,
        rows: Iterable<KategoriRow>
    ) = batchInsert(rows) { row ->
        this[KategorierTable.parentId] = parentId
        this[kode] = row.kode
        this[normalisertKode] = row.normalisertKode
        this[navn] = row.navn
    }
}
