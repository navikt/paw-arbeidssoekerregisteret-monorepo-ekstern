package no.nav.paw.arbeidssoekerregisteret.model

import java.time.Instant
import java.util.*

data class EgenvurderingRow(
    val id: UUID,
    val periodeId: UUID,
    val profileringId: UUID,
    val profilering: ProfilertTil,
    val egenvurdering: ProfilertTil,
    val profileringTidspunkt: Instant,
    val periodeStartetTidspunkt: Instant
)