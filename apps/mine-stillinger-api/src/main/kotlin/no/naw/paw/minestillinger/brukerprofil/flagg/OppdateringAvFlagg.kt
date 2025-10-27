package no.naw.paw.minestillinger.brukerprofil.flagg

data class OppdateringAvFlagg(
    val nyeOgOppdaterteFlagg: List<LagretFlagg>,
    val søkSkalSlettes: Boolean
)

val ingenOppdateringAvFlagg = OppdateringAvFlagg(
    nyeOgOppdaterteFlagg = emptyList(),
    søkSkalSlettes = false
)