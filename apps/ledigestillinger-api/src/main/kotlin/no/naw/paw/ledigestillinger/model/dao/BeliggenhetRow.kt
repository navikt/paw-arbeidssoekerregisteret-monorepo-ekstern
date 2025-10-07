package no.naw.paw.ledigestillinger.model.dao

data class BeliggenhetRow(
    val id: Long,
    val parentId: Long,
    val adresse: String?,
    val postkode: String?,
    val poststed: String?,
    val kommune: String?,
    val fylke: String?,
    val land: String
)
