package no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.DatabaseConfig
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.dataSource
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.migrateDatabase
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import javax.sql.DataSource

fun initTestDatabase(): DataSource {
    val postgres = postgreSQLContainer()
    val dataSource =
        DatabaseConfig(
            host = postgres.host,
            port = postgres.firstMappedPort,
            username = postgres.username,
            password = postgres.password,
            name = postgres.databaseName
        ).dataSource()
    migrateDatabase(dataSource)
    return dataSource
}

fun postgreSQLContainer(): PostgreSQLContainer<out PostgreSQLContainer<*>> {
    val postgres =
        PostgreSQLContainer(
            "postgres:14"
        ).apply {
            addEnv("POSTGRES_PASSWORD", "admin")
            addEnv("POSTGRES_USER", "admin")
            addEnv("POSTGRES_DB", "arbeidssoekerregisteretapioppslag")
            addExposedPorts(5432)
        }
    postgres.start()
    postgres.waitingFor(Wait.forHealthcheck())
    return postgres
}
