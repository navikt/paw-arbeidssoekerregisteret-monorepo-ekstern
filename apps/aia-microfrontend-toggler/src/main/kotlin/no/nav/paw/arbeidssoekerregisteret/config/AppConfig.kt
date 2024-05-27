package no.nav.paw.arbeidssoekerregisteret.config

import java.time.Duration

const val APPLICATION_LOGGER_NAME = "app"
const val APPLICATION_CONFIG_FILE_NAME = "application_configuration.toml"
const val APPLICATION_ID_SUFFIX = "beta"

data class AppConfig(
    val kafkaTopology: KafkaTopologyConfig,
    val kafkaKeys: KafkaKeysConfig,
    val regler: ReglerConfig,
    val appId: String = currentAppId
)

data class KafkaTopologyConfig(
    val periodeTopic: String,
    val vedtakTopic: String,
    val microfrontendTopic: String,
    val toggleStoreName: String,
    val varselStoreName: String
)

data class KafkaKeysConfig(
    val url: String, val scope: String
)

data class ReglerConfig(val utsattDeaktiveringAvAiaMinSide: Duration)