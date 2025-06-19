package no.nav.paw.arbeidssoekerregisteret.config

const val APPLICATION_CONFIG = "application_config.toml"

data class ApplicationConfig(
    val kafkaTopology: KafkaTopologyConfig,
)

data class KafkaTopologyConfig(
    val producerVersion: String,
    val applicationId: String,
    val egenvurderingTopic: String,
)
