package no.nav.paw.arbeidssoekerregisteret.model

import java.time.Instant
import java.util.*

@JvmRecord
data class PeriodeInfo(
    val id: UUID,
    val identitetsnummer: String,
    val arbeidssoekerId: Long,
    val startet: Instant,
    val avsluttet: Instant?
)
