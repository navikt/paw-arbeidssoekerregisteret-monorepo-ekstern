package no.naw.paw.minestillinger.db.ops

import no.nav.paw.database.config.DatabaseConfig
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait

fun postgreSQLContainer(): PostgreSQLContainer<out PostgreSQLContainer<*>> {
    val postgres = PostgreSQLContainer(
        "postgres:18"
    ).apply {
        addEnv("POSTGRES_PASSWORD", "admin")
        addEnv("POSTGRES_USER", "admin")
        addEnv("POSTGRES_DB", "arbeidssoekerregisteretbrukerprofiler")
        addExposedPorts(5432)
    }
    postgres.start()
    postgres.waitingFor(Wait.forHealthcheck())
    return postgres
}

fun databaseConfigFrom(postgres: PostgreSQLContainer<out PostgreSQLContainer<*>>): DatabaseConfig {
    return DatabaseConfig(
        host = postgres.host,
        port = postgres.firstMappedPort,
        username = postgres.username,
        password = postgres.password,
        database = postgres.databaseName
    )
}