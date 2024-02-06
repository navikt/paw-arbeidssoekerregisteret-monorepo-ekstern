package no.nav.paw.arbeidssokerregisteret.arena.adapter.config

data class ApplicationConfig(
    val topics: Topics
)

data class Topics(
    val opplysningerOmArbeidssoeker: String,
    val arbeidssokerperioder: String,
    val profilering: String,
    val arena: String
)