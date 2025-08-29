package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.config

const val APPLICATION_CONFIG = "application_config.toml"

data class ApplicationConfig(
    val kafkaTopology: KafkaTopologyConfig,
    val veilarbdialogClientConfig: VeilarbdialogClientConfig,
)

data class KafkaTopologyConfig(
    val consumerVersion: String,
    val applicationIdPrefix: String,
    val egenvurderingTopic: String,
)

data class VeilarbdialogClientConfig(
    val url: String,
    val scope: String,
)

