package no.nav.paw.ledigestillinger.model.dao

data class EgenskapRow(
    val id: Long,
    val parentId: Long,
    val key: String,
    val value: String
)
