package no.naw.paw.minestillinger

import com.zaxxer.hikari.HikariDataSource
import no.nav.paw.database.config.DatabaseConfig
import no.nav.paw.database.factory.createHikariDataSource
import java.io.Closeable
import java.sql.Connection
import java.sql.SQLException

// Må være stabil mellom versjoner slik at gamle og nye podder konkurrerer om samme lås under utrulling.
private const val VEDLIKEHOLDSLÅS = 7_451_836_291_047_223_619L

interface Vedlikeholdsledervalg : Closeable {
    fun prøvÅBliLeder(): Lederlås?
}

interface Lederlås : Closeable {
    fun erGyldig(): Boolean
}

class PostgresVedlikeholdsledervalg(
    private val dataSource: HikariDataSource
) : Vedlikeholdsledervalg {
    override fun prøvÅBliLeder(): Lederlås? {
        val connection = dataSource.connection
        return try {
            val låst = connection.prepareStatement(
                "SELECT pg_try_advisory_lock(?)"
            ).use { statement ->
                statement.setLong(1, VEDLIKEHOLDSLÅS)
                statement.executeQuery().use { result ->
                    result.next() && result.getBoolean(1)
                }
            }
            if (låst) {
                PostgresLederlås(connection)
            } else {
                connection.close()
                null
            }
        } catch (error: SQLException) {
            connection.close()
            throw error
        }
    }

    override fun close() {
        dataSource.close()
    }
}

private class PostgresLederlås(
    private val connection: Connection
) : Lederlås {
    override fun erGyldig(): Boolean =
        !connection.isClosed && connection.isValid(5)

    override fun close() {
        try {
            if (!connection.isClosed && connection.isValid(5)) {
                connection.prepareStatement(
                    "SELECT pg_advisory_unlock(?)"
                ).use { statement ->
                    statement.setLong(1, VEDLIKEHOLDSLÅS)
                    statement.execute()
                }
            }
        } catch (_: SQLException) {
            appLogger.info("Databasesesjonen for lederlåsen var allerede avsluttet")
        } finally {
            try {
                connection.close()
            } catch (_: SQLException) {
                appLogger.info("Den avsluttede databasesesjonen kunne ikke returneres til tilkoblingspoolen")
            }
        }
    }
}

fun postgresVedlikeholdsledervalg(databaseConfig: DatabaseConfig): Vedlikeholdsledervalg =
    PostgresVedlikeholdsledervalg(
        createHikariDataSource(
            databaseConfig.copy(maximumPoolSize = 1)
        )
    )
