package no.nav.paw.arbeidssoekerregisteret.api.oppslag.config

const val APPLICATION_CONFIG = "application_config.toml"

data class ApplicationConfig(
    val perioderGroupId: String,
    val opplysningerGroupId: String,
    val profileringGroupId: String,
    val bekreftelseGroupId: String,
    val perioderTopic: String,
    val opplysningerTopic: String,
    val profileringTopic: String,
    val bekreftelseTopic: String
)