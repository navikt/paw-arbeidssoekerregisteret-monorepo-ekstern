package no.nav.paw.arbeidssoekerregisteret.model

data class ToggleState(
    val identitetsnummer: String,
    val arbeidssoekerId: Long,
    val microfrontends: Map<MicroFrontendId, MicroFrontendState>
)