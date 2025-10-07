package no.naw.paw.ledigestillinger.config

const val APPLICATION_CONFIG = "application_config.toml"

data class ApplicationConfig(
    val pamStillingEksternTopic: String,
)
