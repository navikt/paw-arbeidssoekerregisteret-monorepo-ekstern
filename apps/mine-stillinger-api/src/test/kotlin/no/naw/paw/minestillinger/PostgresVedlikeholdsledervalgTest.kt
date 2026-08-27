package no.naw.paw.minestillinger

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import no.nav.paw.database.factory.createHikariDataSource
import no.naw.paw.minestillinger.db.ops.databaseConfigFrom
import no.naw.paw.minestillinger.db.ops.postgreSQLContainer

class PostgresVedlikeholdsledervalgTest : FreeSpec({
    val postgres = autoClose(postgreSQLContainer())
    val databaseConfig = databaseConfigFrom(postgres)

    "bare én pod får lederlåsen og en annen kan overta" {
        postgresVedlikeholdsledervalg(databaseConfig).use { førstePod ->
            postgresVedlikeholdsledervalg(databaseConfig).use { andrePod ->
                val førsteLås = førstePod.prøvÅBliLeder()
                førsteLås.shouldNotBeNull()

                andrePod.prøvÅBliLeder().shouldBeNull()

                førsteLås.close()
                andrePod.prøvÅBliLeder().shouldNotBeNull().close()
            }
        }
    }

    "låsen frigjøres når PostgreSQL-sesjonen til lederpoden dør" {
        postgresVedlikeholdsledervalg(databaseConfig).use { førstePod ->
            postgresVedlikeholdsledervalg(databaseConfig).use { andrePod ->
                createHikariDataSource(databaseConfig).use { administrasjon ->
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
                                result.next() shouldBe true
                                result.getBoolean(1) shouldBe true
                            }
                        }
                    }

                    ventPåLås(andrePod).close()
                    førsteLås.close()
                }
            }
        }
    }
})

private fun ventPåLås(ledervalg: Vedlikeholdsledervalg): Lederlås {
    repeat(500) {
        ledervalg.prøvÅBliLeder()?.let { return it }
        Thread.sleep(10)
    }
    error("Fikk ikke lederlåsen innen fem sekunder")
}
