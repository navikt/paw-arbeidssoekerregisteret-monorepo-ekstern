package no.nav.paw.ledigestillinger.model.dao

import org.jetbrains.exposed.v1.core.ResultRow

data class BeliggenhetRow(
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

fun ResultRow.asBeliggenhetRow(): BeliggenhetRow = BeliggenhetRow(
    id = this[BeliggenheterTable.id].value,
    parentId = this[BeliggenheterTable.parentId],
    adresse = this[BeliggenheterTable.adresse],
    postkode = this[BeliggenheterTable.postkode],
    poststed = this[BeliggenheterTable.poststed],
    kommune = this[BeliggenheterTable.kommune],
    kommunekode = this[BeliggenheterTable.kommunekode],
    fylke = this[BeliggenheterTable.fylke],
    fylkeskode = this[BeliggenheterTable.fylkeskode],
    land = this[BeliggenheterTable.land]
)
