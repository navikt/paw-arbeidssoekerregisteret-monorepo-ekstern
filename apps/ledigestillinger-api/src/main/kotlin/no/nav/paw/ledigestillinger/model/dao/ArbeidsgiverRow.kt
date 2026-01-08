package no.nav.paw.ledigestillinger.model.dao

data class ArbeidsgiverRow(
    val id: Long,
    val parentId: Long,
    val orgForm: String,
    val orgNr: String?,
    val parentOrgNr: String?,
    val navn: String,
    val offentligNavn: String
)
