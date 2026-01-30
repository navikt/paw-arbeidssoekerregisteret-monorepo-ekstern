package no.nav.paw.arbeidssoekerregisteret.config

import no.nav.paw.arbeidssoekerregisteret.model.Sensitivitet
import java.time.Duration
import java.time.Instant

const val APPLICATION_CONFIG = "application_config.toml"

data class ApplicationConfig(
    val kafkaTopology: KafkaTopologyConfig,
    val microfrontendToggle: MicrofrontendToggleConfig,
    val deprekering: DeprekeringConfig
)

data class KafkaTopologyConfig(
    val shutDownTimeout: Duration,
    val periodeTopic: String,
    val siste14aVedtakTopic: String,
    val beriket14aVedtakTopic: String,
    val microfrontendTopic: String,
    val periodeStreamIdSuffix: String,
    val siste14aVedtakStreamIdSuffix: String,
    val toggleProducerIdSuffix: String,
    val periodeStateStore: String,
    val toggleStateStore: String
)

data class MicrofrontendToggleConfig(
    val aiaMinSide: String,
    val aiaMinSideSensitivitet: Sensitivitet,
    val aiaBehovsvurdering: String,
    val aiaBehovsvurderingSensitivitet: Sensitivitet,
    val periodeTogglePunctuatorSchedule: Duration,
    val utsattDeaktiveringAvAiaMinSide: Duration
)

data class DeprekeringConfig(
    val aktivert: Boolean,
    val tidspunkt: Instant,
    val csvFil: String
)