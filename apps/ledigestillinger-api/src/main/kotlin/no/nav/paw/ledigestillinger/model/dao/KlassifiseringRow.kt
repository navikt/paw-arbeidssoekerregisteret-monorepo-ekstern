package no.nav.paw.ledigestillinger.model.dao

import org.jetbrains.exposed.v1.core.ResultRow

data class KlassifiseringRow(
    val id: Long,
    val parentId: Long,
    val type: String,
    val kode: String,
    val navn: String
)

fun ResultRow.asKlassifiseringRow(): KlassifiseringRow = KlassifiseringRow(
    id = this[KlassifiseringerTable.id].value,
    parentId = this[KlassifiseringerTable.parentId],
    type = this[KlassifiseringerTable.type],
    kode = this[KlassifiseringerTable.kode],
    navn = this[KlassifiseringerTable.navn]
)