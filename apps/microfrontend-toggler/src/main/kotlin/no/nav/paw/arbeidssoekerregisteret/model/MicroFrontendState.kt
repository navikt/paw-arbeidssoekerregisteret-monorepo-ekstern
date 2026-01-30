package no.nav.paw.arbeidssoekerregisteret.model

import java.time.Instant

data class MicroFrontendState(
    val status: ToggleStatus = ToggleStatus.UKJENT_VERDI,
    val updated: Instant = Instant.now()
)
