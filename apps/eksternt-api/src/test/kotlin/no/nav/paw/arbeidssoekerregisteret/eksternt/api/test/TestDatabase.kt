package no.nav.paw.arbeidssoekerregisteret.eksternt.api.test

import no.nav.paw.arbeidssoekerregisteret.eksternt.api.config.DatabaseConfig
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils.dataSource
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils.migrateDatabase
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
            addEnv("POSTGRES_USER", "eksternt_api")
            addEnv("POSTGRES_PASSWORD", "Paw1234")
            addEnv("POSTGRES_DB", "arbeidssoekerregisteretapiekstern")
            addExposedPorts(5432)
        }
    postgres.start()
    postgres.waitingFor(Wait.forHealthcheck())
    return postgres
}
