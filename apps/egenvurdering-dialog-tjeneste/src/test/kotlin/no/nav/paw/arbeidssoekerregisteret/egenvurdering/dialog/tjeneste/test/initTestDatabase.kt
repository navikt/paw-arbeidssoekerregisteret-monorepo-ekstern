package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.test

import com.zaxxer.hikari.HikariDataSource
import no.nav.paw.database.config.DatabaseConfig
import no.nav.paw.database.factory.createHikariDataSource
import org.flywaydb.core.Flyway
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait

fun buildPostgresDataSource(): HikariDataSource {
    val postgres = postgreSQLContainer()
    val databaseConfig = DatabaseConfig(
        host = postgres.host,
        port = postgres.firstMappedPort,
        username = postgres.username,
        password = postgres.password,
        database = postgres.databaseName
    )
    val dataSource = createHikariDataSource(databaseConfig)
    Flyway.configure()
        .baselineOnMigrate(true)
        .dataSource(dataSource)
        .load()
        .migrate()
    return dataSource
}

private fun postgreSQLContainer(): PostgreSQLContainer<out PostgreSQLContainer<*>> {
    val postgres =
        PostgreSQLContainer(
            "postgres:17"
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
