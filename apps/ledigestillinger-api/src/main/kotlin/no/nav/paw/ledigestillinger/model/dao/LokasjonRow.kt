package no.nav.paw.ledigestillinger.model.dao

import org.jetbrains.exposed.v1.core.ResultRow

data class LokasjonRow(
    val id: Long,
    val parentId: Long,
    val adresse: String?,
    val postkode: String?,
    val poststed: String?,
    val kommune: String?,
    val kommunekode: String?,
    val fylke: String?,
    val fylkeskode: String?,
    val land: String
)

fun ResultRow.asLokasjonRow(): LokasjonRow = LokasjonRow(
    id = this[LokasjonerTable.id].value,
    parentId = this[LokasjonerTable.parentId],
    adresse = this[LokasjonerTable.adresse],
    postkode = this[LokasjonerTable.postkode],
    poststed = this[LokasjonerTable.poststed],
    kommune = this[LokasjonerTable.kommune],
    kommunekode = this[LokasjonerTable.kommunekode],
    fylke = this[LokasjonerTable.fylke],
    fylkeskode = this[LokasjonerTable.fylkeskode],
    land = this[LokasjonerTable.land]
)
