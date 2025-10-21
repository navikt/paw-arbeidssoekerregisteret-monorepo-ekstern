package no.nav.paw.ledigestillinger.model.dao

import org.jetbrains.exposed.v1.core.ResultRow

data class KategoriRow(
    val id: Long,
    val parentId: Long,
    val kode: String,
    val normalisertKode: String,
    val navn: String
)

fun ResultRow.asKategoriRow(): KategoriRow = KategoriRow(
    id = this[KategorierTable.id].value,
    parentId = this[KategorierTable.parentId],
    kode = this[KategorierTable.kode],
    normalisertKode = this[KategorierTable.normalisertKode],
    navn = this[KategorierTable.navn]
)