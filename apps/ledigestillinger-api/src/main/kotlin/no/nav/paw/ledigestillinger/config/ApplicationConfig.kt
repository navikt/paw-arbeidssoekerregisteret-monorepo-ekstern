package no.nav.paw.ledigestillinger.config

const val APPLICATION_CONFIG = "application_config.toml"

data class ApplicationConfig(
    val pamStillingerKafkaConsumer: KafkaConsumerConfig
)

data class KafkaConsumerConfig(
    val version: Int,
    val topic: String,
    val groupIdPrefix: String,
) {
    val groupId: String get() = "$groupIdPrefix-v$version"
    val clientId: String get() = "$groupIdPrefix-v$version-consumer"
}