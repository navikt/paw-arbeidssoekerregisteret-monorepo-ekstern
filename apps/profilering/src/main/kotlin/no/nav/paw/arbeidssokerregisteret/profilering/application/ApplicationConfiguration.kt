package no.nav.paw.arbeidssokerregisteret.profilering.application

const val APPLICATION_CONFIG_FILE = "application_config.toml"
data class ApplicationConfiguration(
    val periodeTopic: String,
    val opplysningerTopic: String,
    val profileringTopic: String,
    val profileringGrunnlagTopic: String,
    val applicationIdSuffix: String,
    val joiningStateStoreName: String
)