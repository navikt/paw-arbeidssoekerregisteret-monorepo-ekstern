package no.nav.paw.arbeidssoekerregisteret.config

import no.nav.paw.arbeidssoekerregisteret.texas.TexasClientConfig

const val APPLICATION_CONFIG = "application_config.toml"

data class ApplicationConfig(
    val kafkaTopology: KafkaTopologyConfig,
    val oppslagApiConfig: OppslagApiConfig,
    val texasClientConfig: TexasClientConfig,
)

data class KafkaTopologyConfig(
    val producerVersion: String,
    val applicationIdPrefix: String,
    val egenvurderingTopic: String,
)

data class OppslagApiConfig(
    val url: String,
    val scope: String,
)

