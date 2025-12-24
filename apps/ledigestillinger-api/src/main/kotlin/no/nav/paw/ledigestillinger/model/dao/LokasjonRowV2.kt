package no.nav.paw.ledigestillinger.model.dao

import org.jetbrains.exposed.v1.core.ResultRow

fun ResultRow.asLokasjonRowV2(): LokasjonRow = LokasjonRow(
    id = this[LokasjonerTableV2.id].value,
    parentId = this[LokasjonerTableV2.parentId],
    adresse = this[LokasjonerTableV2.adresse],
    postkode = this[LokasjonerTableV2.postkode],
    poststed = this[LokasjonerTableV2.poststed],
    kommune = this[LokasjonerTableV2.kommune],
    kommunekode = this[LokasjonerTableV2.kommunekode],
    fylke = this[LokasjonerTableV2.fylke],
    fylkeskode = this[LokasjonerTableV2.fylkeskode],
    land = this[LokasjonerTableV2.land]
)
