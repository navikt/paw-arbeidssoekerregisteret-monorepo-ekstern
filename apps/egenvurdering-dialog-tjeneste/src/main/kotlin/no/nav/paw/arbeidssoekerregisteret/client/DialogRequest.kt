package no.nav.paw.arbeidssoekerregisteret.client

data class DialogRequest(
    val fnr: String? = null,
    val dialogId: String? = null,
    val overskrift: String,
    val tekst: String,
)
