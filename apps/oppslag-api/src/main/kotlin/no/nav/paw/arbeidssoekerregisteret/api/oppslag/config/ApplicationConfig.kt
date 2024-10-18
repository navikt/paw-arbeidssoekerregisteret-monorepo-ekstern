package no.nav.paw.arbeidssoekerregisteret.api.oppslag.config

const val APPLICATION_CONFIG_FILE_NAME = "application_config.toml"

data class ApplicationConfig(
    val gruppeId: String,
    val periodeTopic: String,
    val opplysningerOmArbeidssoekerTopic: String,
    val profileringTopic: String,
    val bekreftelseTopic: String,
    val authProviders: List<AuthProvider>,
    val poaoClientConfig: ServiceClientConfig,
    val database: DatabaseConfig
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

data class ServiceClientConfig(
    val url: String,
    val scope: String
)

data class DatabaseConfig(
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val name: String
) {
    val url get() = "jdbc:postgresql://$host:$port/$name?user=$username&password=$password"
}
