package no.nav.paw.ledigestillinger.config

import java.time.Duration
import java.time.Instant

const val APPLICATION_CONFIG = "application_config.toml"

data class ApplicationConfig(
    val beholdAlleStillingerPublisertEtter: Instant,
    val beholdIkkeAktiveStillingerMedUtloeperNyereEnn: Duration,
    val pamStillingerKafkaConsumer: KafkaConsumerConfig,
    val databaseMetricsScheduledTask: ScheduledTaskConfig,
    val databaseCleanupScheduledTask: ScheduledTaskConfig
)

data class KafkaConsumerConfig(
    val version: Int,
    val topic: String,
    val groupIdPrefix: String,
) {
    val groupId: String get() = "$groupIdPrefix-v$version"
    val clientId: String get() = "$groupIdPrefix-v$version-consumer"
}

data class ScheduledTaskConfig(
    val name: String,
    val delay: Duration,
    val period: Duration
)