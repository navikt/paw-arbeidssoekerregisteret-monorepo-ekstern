package no.naw.paw.minestillinger.domain

import java.time.Instant
import java.util.UUID

data class Profilering(
    val id: Long,
    val periodeId: PeriodeId,
    val profileringId: UUID,
    val profileringTidspunkt: Instant,
    val profileringResultat: ProfileringResultat
)