package no.naw.paw.minestillinger.brukerprofil.direktemeldte

data class DirekteMeldteStillingerConfig(
    val url: String,
    val scope: String,
    val funksjonsnivaa: DirekteMeldteStillingerFunksjonsnivaa
)