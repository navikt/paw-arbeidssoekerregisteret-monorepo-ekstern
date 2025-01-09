package no.nav.paw.arbeidssoekerregisteret.eksternt.api.config

const val APPLICATION_CONFIG = "application_config.toml"

data class ApplicationConfig(
    val gruppeId: String,
    val periodeTopic: String
)
