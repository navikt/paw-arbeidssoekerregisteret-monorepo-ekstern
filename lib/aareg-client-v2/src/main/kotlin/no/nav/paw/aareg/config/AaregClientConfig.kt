package no.nav.paw.aareg.config

const val AAREG_CLIENT_CONFIG = "aareg_client_config.toml"

data class AaregClientConfig(
    val url: String,
    val scope: String
)
