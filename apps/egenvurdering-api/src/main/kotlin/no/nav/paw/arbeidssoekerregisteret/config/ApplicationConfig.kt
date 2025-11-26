package no.nav.paw.arbeidssoekerregisteret.config

import java.time.Instant

const val APPLICATION_CONFIG = "application_config.toml"

data class ApplicationConfig(
    val producerConfig: ProducerConfig,
    val prodsettingstidspunktEgenvurdering: Instant
)

data class ProducerConfig(
    val producerVersion: String,
    val applicationIdPrefix: String,
    val egenvurderingTopic: String,
)
