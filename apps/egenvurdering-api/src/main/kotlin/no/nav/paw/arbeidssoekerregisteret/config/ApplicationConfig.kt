package no.nav.paw.arbeidssoekerregisteret.config

const val APPLICATION_CONFIG = "application_config.toml"

data class ApplicationConfig(
    val kafkaTopology: KafkaTopologyConfig,
)

data class KafkaTopologyConfig(
    val periodeTopic: String,
    val beriket14aVedtakTopic: String,
)

