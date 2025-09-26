package no.nav.paw.arbeidssoekerregisteret.config

import no.nav.paw.security.texas.TexasClientConfig

const val APPLICATION_CONFIG = "application_config.toml"

data class ApplicationConfig(
    val producerConfig: ProducerConfig,
    val texasClientConfig: TexasClientConfig
)

data class ProducerConfig(
    val producerVersion: String,
    val applicationIdPrefix: String,
    val egenvurderingTopic: String,
)
