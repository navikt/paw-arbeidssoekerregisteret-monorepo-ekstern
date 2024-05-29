package no.nav.paw.arbeidssoekerregisteret.config

import no.nav.paw.config.env.NaisEnv
import no.nav.paw.config.env.currentAppId
import no.nav.paw.config.env.currentNaisEnv
import java.time.Duration

const val APPLICATION_LOGGER_NAME = "no.nav.paw.application"
const val APPLICATION_CONFIG_FILE_NAME = "application_configuration.toml"
const val KAFKA_KEY_CONFIG_FILE_NAME = "kafka_key_configuration.toml"
const val AZURE_M2M_CONFIG_FILE_NAME = "azure_m2m_configuration.toml"
const val KAFKA_PRODUCER_APPLICATION_ID_SUFFIX = "producer-v1"
const val KAFKA_STREAMS_APPLICATION_ID_SUFFIX = "stream-v1"

data class AppConfig(
    val kafkaTopology: KafkaTopologyConfig,
    val regler: ReglerConfig,
    val microfrontends: MicrofrontendsConfig,
    val appId: String = currentAppId,
    val naisEnv: NaisEnv = currentNaisEnv
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