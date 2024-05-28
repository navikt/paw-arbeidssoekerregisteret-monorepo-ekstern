package no.nav.paw.arbeidssoekerregisteret.model

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
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

fun buildPeriodeInfo(periode: Periode, arbeidssoekerId: Long): PeriodeInfo {
    return PeriodeInfo(
        id = periode.id,
        identitetsnummer = periode.identitetsnummer,
        arbeidssoekerId = arbeidssoekerId,
        startet = periode.startet.tidspunkt,
        avsluttet = periode.avsluttet?.tidspunkt
    )
}
