package no.nav.paw.arbeidssoekerregisteret.api.oppslag.config

import java.time.Duration

const val APPLICATION_CONFIG = "application_config.toml"

data class ApplicationConfig(
    val perioderGroupId: String,
    val opplysningerGroupId: String,
    val profileringGroupId: String,
    val bekreftelseGroupId: String,
    val perioderTopic: String,
    val opplysningerTopic: String,
    val profileringTopic: String,
    val bekreftelseTopic: String,
    val perioderMetricsTaskDelay: Duration = Duration.ZERO,
    val perioderMetricsTaskInterval: Duration = Duration.ofMinutes(10)
)