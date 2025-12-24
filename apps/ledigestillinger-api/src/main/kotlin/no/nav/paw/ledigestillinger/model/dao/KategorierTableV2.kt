package no.nav.paw.ledigestillinger.model.dao

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.selectAll

object KategorierTableV2 : LongIdTable("kategorier_v2") {
    val parentId = long("parent_id").references(StillingerTableV2.id)
    val kode = varchar("kode", 20)
    val normalisertKode = varchar("normalisert_kode", 20)
    val navn = varchar("navn", 255)

    fun selectRowsByParentId(
        parentId: Long
    ): List<KategoriRow> = selectAll()
        .where { KategorierTableV2.parentId eq parentId }
        .map { it.asKategoriRowV2() }

    fun insert(
        parentId: Long,
        rows: Iterable<KategoriRow>
    ) = batchInsert(rows) { row ->
        this[KategorierTableV2.parentId] = parentId
        this[kode] = row.kode
        this[normalisertKode] = row.normalisertKode
        this[navn] = row.navn
    }
}
