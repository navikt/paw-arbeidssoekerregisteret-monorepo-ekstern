package no.nav.paw.arbeidssoekerregisteret.config

import no.nav.paw.config.env.currentAppId
import no.nav.paw.kafkakeygenerator.client.KafkaKeyConfig
import java.time.Duration

const val APPLICATION_LOGGER_NAME = "no.nav.paw.application"
const val APPLICATION_CONFIG_FILE_NAME = "application_configuration.toml"
const val APPLICATION_ID_SUFFIX = "beta"

data class AppConfig(
    val kafkaTopology: KafkaTopologyConfig,
    val kafkaKeys: KafkaKeyConfig,
    val regler: ReglerConfig,
    val microfrontends: MicrofrontendsConfig,
    val appId: String = currentAppId
)

data class KafkaTopologyConfig(
    val periodeTopic: String,
    val vedtakTopic: String,
    val microfrontendTopic: String,
    val toggleStoreName: String,
    val varselStoreName: String,
    val periodeToggleProcessor: String
)

data class ReglerConfig(
    val periodeTogglePunctuatorSchedule: Duration,
    val utsattDeaktiveringAvAiaMinSide: Duration
)

data class MicrofrontendsConfig(
    val aiaMinSide: String,
    val aiaBehovsvurdering: String
)