package no.nav.paw.poao.tilgang.config

const val POAO_TILGANG_CLIENT_CONFIG = "poao_tilgang_client_config.toml"

data class PoaoTilgangClientConfig(
    val url: String,
    val scope: String,
)
