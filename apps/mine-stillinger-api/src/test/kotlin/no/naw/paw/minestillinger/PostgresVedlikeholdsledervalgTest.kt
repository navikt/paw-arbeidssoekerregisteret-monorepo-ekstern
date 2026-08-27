package no.naw.paw.minestillinger

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import no.nav.paw.database.factory.createHikariDataSource
import no.naw.paw.minestillinger.db.ops.databaseConfigFrom
import no.naw.paw.minestillinger.db.ops.postgreSQLContainer

class PostgresVedlikeholdsledervalgTest : FreeSpec({
    val postgres = autoClose(postgreSQLContainer())
    val databaseConfig = databaseConfigFrom(postgres)
    val førstePod = autoClose(postgresVedlikeholdsledervalg(databaseConfig))
    val andrePod = autoClose(postgresVedlikeholdsledervalg(databaseConfig))
    val administrasjon = autoClose(createHikariDataSource(databaseConfig))

    "bare én pod får lederlåsen og en annen kan overta" {
        val førsteLås = førstePod.prøvÅBliLeder()
        førsteLås.shouldNotBeNull()

        andrePod.prøvÅBliLeder().shouldBeNull()

        førsteLås.close()
        andrePod.prøvÅBliLeder().shouldNotBeNull().close()
    }

    "låsen frigjøres når PostgreSQL-sesjonen til lederpoden dør" {
        val førsteLås = førstePod.prøvÅBliLeder()
        førsteLås.shouldNotBeNull()

        administrasjon.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT pg_terminate_backend(pid)
                FROM pg_locks
                WHERE locktype = 'advisory'
                  AND granted
                  AND pid <> pg_backend_pid()
                """.trimIndent()
            ).use { statement ->
                statement.executeQuery().use { result ->
                    result.next()
                }
            }
        }

        andrePod.prøvÅBliLeder().shouldNotBeNull().close()
        førsteLås.close()
    }
})
