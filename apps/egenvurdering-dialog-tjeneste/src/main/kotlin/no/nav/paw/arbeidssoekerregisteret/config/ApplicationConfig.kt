package no.nav.paw.arbeidssoekerregisteret.config

const val APPLICATION_CONFIG = "application_config.toml"

data class ApplicationConfig(
    val kafkaTopology: KafkaTopologyConfig,
    val dialogClientConfig: DialogClientConfig,
)

data class KafkaTopologyConfig(
    val consumerVersion: String,
    val applicationIdPrefix: String,
    val egenvurderingTopic: String,
)

data class DialogClientConfig(
    val url: String,
    val scope: String,
)

