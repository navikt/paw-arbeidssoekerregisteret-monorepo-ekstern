package no.naw.paw.brukerprofiler.domain

import java.time.Instant
import java.util.UUID

data class Profilering(
    val id: Long,
    val periodeId: UUID,
    val profileringId: UUID,
    val profileringTidspunkt: Instant,
    val profileringResultat: ProfileringResultat
)