package no.nav.paw.arbeidssoekerregisteret.config

const val APPLICATION_CONFIG = "application_config.toml"

data class ApplicationConfig(
    val producerConfig: ProducerConfig,
)

data class ProducerConfig(
    val producerVersion: String,
    val applicationIdPrefix: String,
    val egenvurderingTopic: String,
)
