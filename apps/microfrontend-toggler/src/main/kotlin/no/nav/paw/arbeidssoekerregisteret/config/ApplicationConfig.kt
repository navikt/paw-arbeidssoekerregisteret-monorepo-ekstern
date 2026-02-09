package no.nav.paw.arbeidssoekerregisteret.config

import java.time.Duration

const val APPLICATION_CONFIG = "application_config.toml"

data class ApplicationConfig(
    val kafkaTopology: KafkaTopologyConfig,
    val microfrontendToggle: MicrofrontendToggleConfig
)

data class KafkaTopologyConfig(
    val shutDownTimeout: Duration,
    val periodeTopic: String,
    val microfrontendTopic: String,
    val periodeStreamIdSuffix: String,
    val toggleProducerIdSuffix: String,
    val periodeStateStore: String
)

data class MicrofrontendToggleConfig(
    val periodeTogglePunctuatorSchedule: Duration,
    val utsattDeaktiveringAvAiaMinSide: Duration
)