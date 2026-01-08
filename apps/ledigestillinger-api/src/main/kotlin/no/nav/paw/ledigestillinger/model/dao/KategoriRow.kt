package no.nav.paw.ledigestillinger.model.dao

data class KategoriRow(
    val id: Long,
    val parentId: Long,
    val kode: String,
    val normalisertKode: String,
    val navn: String
)
