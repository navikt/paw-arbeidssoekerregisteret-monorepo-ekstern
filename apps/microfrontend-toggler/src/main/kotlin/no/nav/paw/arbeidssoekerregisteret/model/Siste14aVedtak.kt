package no.nav.paw.arbeidssoekerregisteret.model

import no.nav.common.types.identer.AktorId
import java.time.Instant

data class Siste14aVedtak(
    val aktorId: AktorId,
    val innsatsgruppe: String,
    val hovedmal: String,
    val fattetDato: Instant,
    val fraArena: Boolean
)
