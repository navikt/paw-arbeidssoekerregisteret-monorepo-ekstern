package no.nav.paw.arbeidssoekerregisteret.client

data class DialogRequest(
    val tekst: String,
    val dialogId: String? = null,
    val overskrift: String,
    val aktivitetId: String, // TODO: er denne optional?
    val venterPaaSvarFraNav: Boolean,
    val venterPaaSvarFraBruker: Boolean,
    val egenskaper: List<String>, // TODO: er denne optional?
    val fnr: String,
)