package no.nav.paw.arbeidssoekerregisteret.config

const val APPLICATION_CONFIG = "application_config.toml"

data class ApplicationConfig(
    val kafkaTopology: KafkaTopologyConfig,
    val oppslagApiConfig: OppslagApiConfig,
    val texasClientConfig: TexasClientConfig,
)

data class KafkaTopologyConfig(
    val producerVersion: String,
    val applicationId: String,
    val egenvurderingTopic: String,
)

data class OppslagApiConfig(
    val url: String,
    val scope: String,
)

data class TexasClientConfig(
    val endpoint: String,
    val target: String,
    val identityProvider: String = "tokenx",
)
