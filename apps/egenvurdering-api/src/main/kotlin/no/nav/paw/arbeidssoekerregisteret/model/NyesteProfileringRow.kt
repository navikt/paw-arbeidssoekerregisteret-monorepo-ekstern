package no.nav.paw.arbeidssoekerregisteret.model

import java.time.Instant
import java.util.*

data class NyesteProfileringRow(
    val id: UUID,
    val profilertTil: ProfilertTil,
    val profileringTidspunkt: Instant,
    val periodeStartetTidspunkt: Instant,
)
