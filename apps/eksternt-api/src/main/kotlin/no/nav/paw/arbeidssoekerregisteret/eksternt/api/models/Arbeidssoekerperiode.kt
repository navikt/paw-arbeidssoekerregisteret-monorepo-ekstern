package no.nav.paw.arbeidssoekerregisteret.eksternt.api.models

import java.time.Instant
import java.util.*

data class Arbeidssoekerperiode(
    val identitetsnummer: Identitetsnummer,
    val periodeId: UUID,
    val startet: Instant,
    val avsluttet: Instant? = null
)
