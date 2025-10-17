package no.nav.paw.ledigestillinger.config

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

const val APPLICATION_CONFIG = "application_config.toml"

data class ApplicationConfig(
    val pamStillingerKafkaConsumer: KafkaConsumerConfig,
    val velgStillingerNyereEnn: Instant = LocalDateTime.of(2024, 1, 1, 0, 0, 0).toInstant(ZoneOffset.UTC)
)

data class KafkaConsumerConfig(
    val version: Int,
    val topic: String,
    val groupIdPrefix: String,
) {
    val groupId: String get() = "$groupIdPrefix-v$version"
    val clientId: String get() = "$groupIdPrefix-v$version-consumer"
}