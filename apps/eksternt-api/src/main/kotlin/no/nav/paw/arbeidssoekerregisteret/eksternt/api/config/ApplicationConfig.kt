package no.nav.paw.arbeidssoekerregisteret.eksternt.api.config

const val APPLICATION_CONFIG_FILE = "application_config.toml"

data class ApplicationConfiguration(
    val gruppeId: String,
    val periodeTopic: String,
    val database: DatabaseConfig
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
