package no.nav.paw.arbeidssoekerregisteret.texas

data class TexasClientConfig(
    val endpoint: String,
    val target: String,
    val identityProvider: String = "tokenx",
)