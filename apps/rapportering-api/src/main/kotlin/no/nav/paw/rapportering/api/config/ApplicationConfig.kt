package no.nav.paw.rapportering.api.config

const val CONFIG_FILE_NAME = "application_config.toml"

data class ApplicationConfig(
    val applicationIdSuffix: String,
    val producerId: String,
    val rapporteringTopic: String,
    val rapporteringHendelseLoggTopic: String,
    val stateStoreName: String,
    val authProviders: AuthProviders,
    val kafkaKeyGeneratorClient: ServiceClient
)

data class AuthProvider(
    val name: String,
    val discoveryUrl: String,
    val tokenEndpointUrl: String,
    val clientId: String,
    val claims: Claims
)

typealias AuthProviders = List<AuthProvider>

data class Claims(
    val map: List<String>,
    val combineWithOr: Boolean = false
)

data class ServiceClient(
    val url: String,
    val scope: String
)