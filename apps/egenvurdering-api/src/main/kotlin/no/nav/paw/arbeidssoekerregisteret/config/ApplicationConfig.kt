package no.nav.paw.arbeidssoekerregisteret.config

const val APPLICATION_CONFIG = "application_config.toml"

data class ApplicationConfig(
    val kafkaTopology: KafkaTopologyConfig,
)

data class KafkaTopologyConfig(
    val producerVersion: String,
    val applicationId: String,
    val periodeStreamIdSuffix: String,
    val profileringStreamIdSuffix: String,
    val egenvurderingStreamIdSuffix: String,
    val periodeTopic: String,
    val profileringTopic: String,
    val egenvurderingTopic: String,
    val egenvurderingStateStoreName: String,
    val profileringStateStoreName: String,
)
