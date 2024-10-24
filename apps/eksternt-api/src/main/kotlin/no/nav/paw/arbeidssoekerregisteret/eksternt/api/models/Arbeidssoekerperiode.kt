package no.nav.paw.arbeidssoekerregisteret.eksternt.api.models

import java.time.LocalDateTime
import java.util.*

data class Arbeidssoekerperiode(
    val identitetsnummer: Identitetsnummer,
    val periodeId: UUID,
    val startet: LocalDateTime,
    val avsluttet: LocalDateTime? = null
)
