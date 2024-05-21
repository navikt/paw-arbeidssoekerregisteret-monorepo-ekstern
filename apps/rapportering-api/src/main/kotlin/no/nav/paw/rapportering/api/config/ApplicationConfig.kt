package no.nav.paw.rapportering.api.config

const val CONFIG_FILE_NAME = "application_config.toml"

data class ApplicationConfig(
    val authProviders: AuthProviders
)

data class AuthProviders(
    val azure: AuthProvider,
    val tokenx: AuthProvider
)

data class AuthProvider(
    val name: String,
    val discoveryUrl: String,
    val tokenEndpointUrl: String,
    val clientId: String,
    val claims: Claims
)

data class Claims(
    val map: List<String>,
    val combineWithOr: Boolean = false
)