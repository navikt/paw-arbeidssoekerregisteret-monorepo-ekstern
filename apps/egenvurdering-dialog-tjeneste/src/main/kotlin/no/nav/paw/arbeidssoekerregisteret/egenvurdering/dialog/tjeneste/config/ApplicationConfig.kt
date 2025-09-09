package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.config

import no.nav.paw.security.texas.TexasClientConfig

const val APPLICATION_CONFIG = "application_config.toml"

data class ApplicationConfig(
    val kafkaTopology: KafkaTopologyConfig,
    val veilarbdialogClientConfig: VeilarbdialogClientConfig,
    val texasClientConfig: TexasClientConfig
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

