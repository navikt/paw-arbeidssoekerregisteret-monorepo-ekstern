package no.nav.paw.ledigestillinger.model.dao

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
