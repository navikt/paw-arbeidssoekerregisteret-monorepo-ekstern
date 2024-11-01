package no.nav.paw.arbeidssoekerregisteret.config

import java.time.Duration

const val APPLICATION_CONFIG = "application_config.toml"

data class ApplicationConfig(
    val kafkaTopology: KafkaTopologyConfig,
    val microfrontendToggle: MicrofrontendToggleConfig
)

data class KafkaTopologyConfig(
    val shutDownTimeout: Duration,
    val periodeStreamIdSuffix: String,
    val siste14aVedtakStreamIdSuffix: String,
    val periodeTopic: String,
    val siste14aVedtakTopic: String,
    val beriket14aVedtakTopic: String,
    val microfrontendTopic: String,
    val periodeStoreName: String,
    val toggleProducerIdSuffix: String
)

data class MicrofrontendToggleConfig(
    val aiaMinSide: String,
    val aiaBehovsvurdering: String,
    val periodeTogglePunctuatorSchedule: Duration,
    val utsattDeaktiveringAvAiaMinSide: Duration
)
