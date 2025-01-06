package no.nav.paw.arbeidssoekerregisteret.eksternt.api.config

const val DATABASE_CONFIG = "database_config.toml"

data class DatabaseConfig(
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val name: String
) {
    val url get() = "jdbc:postgresql://$host:$port/$name?user=$username&password=$password"
}
