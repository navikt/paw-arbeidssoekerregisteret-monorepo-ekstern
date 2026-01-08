package no.nav.paw.ledigestillinger.model.dao

import no.naw.paw.ledigestillinger.model.KlassifiseringType

data class KlassifiseringRow(
    val id: Long,
    val parentId: Long,
    val type: KlassifiseringType,
    val kode: String,
    val navn: String
)
