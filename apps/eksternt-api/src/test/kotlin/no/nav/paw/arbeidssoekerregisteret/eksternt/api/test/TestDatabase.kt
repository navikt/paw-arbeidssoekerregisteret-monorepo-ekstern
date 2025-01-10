package no.nav.paw.arbeidssoekerregisteret.eksternt.api.test

import no.nav.paw.arbeidssoekerregisteret.eksternt.api.config.DatabaseConfig
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils.generateDatasource
import org.flywaydb.core.Flyway
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import javax.sql.DataSource

fun initTestDatabase(): DataSource {
    val postgres = postgreSQLContainer()
    val databaseConfig = DatabaseConfig(
        host = postgres.host,
        port = postgres.firstMappedPort,
        username = postgres.username,
        password = postgres.password,
        name = postgres.databaseName
    )
    val dataSource = generateDatasource(databaseConfig)
    Flyway.configure()
        .baselineOnMigrate(true)
        .dataSource(dataSource)
        .load()
        .migrate()
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
