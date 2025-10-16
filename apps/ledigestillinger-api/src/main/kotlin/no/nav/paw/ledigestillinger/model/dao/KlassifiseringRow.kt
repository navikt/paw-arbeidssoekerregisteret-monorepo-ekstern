package no.nav.paw.ledigestillinger.model.dao

data class KlassifiseringRow(
    val id: Long,
    val parentId: Long,
    val type: String, // TODO: Enum?
    val kode: String,
    val navn: String
)
