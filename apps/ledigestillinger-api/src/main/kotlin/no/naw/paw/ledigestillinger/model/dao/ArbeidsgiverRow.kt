package no.naw.paw.ledigestillinger.model.dao

data class ArbeidsgiverRow(
    val id: Long,
    val parentId: Long,
    val orgNr: String,
    val navn: String,
    val beskrivelse: String
)
