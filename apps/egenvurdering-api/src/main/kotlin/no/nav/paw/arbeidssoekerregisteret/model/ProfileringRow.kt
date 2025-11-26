package no.nav.paw.arbeidssoekerregisteret.model

import java.util.*

data class ProfileringRow(
    val id: UUID,
    val periodeId: UUID,
    val profilertTil: ProfilertTil
)