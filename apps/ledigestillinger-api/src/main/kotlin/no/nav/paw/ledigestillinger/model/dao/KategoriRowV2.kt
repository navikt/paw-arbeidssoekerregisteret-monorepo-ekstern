package no.nav.paw.ledigestillinger.model.dao

import org.jetbrains.exposed.v1.core.ResultRow

fun ResultRow.asKategoriRowV2(): KategoriRow = KategoriRow(
    id = this[KategorierTableV2.id].value,
    parentId = this[KategorierTableV2.parentId],
    kode = this[KategorierTableV2.kode],
    normalisertKode = this[KategorierTableV2.normalisertKode],
    navn = this[KategorierTableV2.navn]
)