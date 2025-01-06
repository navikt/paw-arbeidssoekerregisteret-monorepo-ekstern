package no.nav.paw.arbeidssoekerregisteret.eksternt.api.models

import java.time.Instant
import java.util.*

data class PeriodeRow(
    val id: Long,
    val identitetsnummer: String,
    val periodeId: UUID,
    val startet: Instant,
    val avsluttet: Instant? = null
)
