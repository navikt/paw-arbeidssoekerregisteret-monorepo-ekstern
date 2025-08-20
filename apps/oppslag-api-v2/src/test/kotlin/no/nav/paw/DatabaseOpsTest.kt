package no.nav.paw

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssokerregisteret.standardTopicNames
import no.nav.paw.config.env.Local
import no.nav.paw.database.config.DatabaseConfig
import no.nav.paw.model.Identitetsnummer
import no.nav.paw.oppslagapi.data.consumer.writeBatchToDb
import no.nav.paw.oppslagapi.data.query.ExposedDatabaseQuerySupport
import no.nav.paw.oppslagapi.initDatabase
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Serde
import org.jetbrains.exposed.sql.transactions.transaction
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.utility.DockerImageName

const val database = "paw"
const val username = "paw_user"
const val password = "paw_password"
val topicNames = standardTopicNames(Local)
val serde: Serde<SpecificRecord> = opprettSerde()

class DatabaseOpsTest: FreeSpec({
    "Database Ops" - {
        val dbContainer = PostgreSQLContainer(DockerImageName.parse("postgres:17"))
            .withDatabaseName(database)
            .withUsername(username)
            .withPassword(password)
            .withExposedPorts(5432)
            .waitingFor(HostPortWaitStrategy())
        dbContainer.start()
        val dbConfig = DatabaseConfig(
            username = username,
            password = password,
            database = database,
            host = dbContainer.host,
            port = dbContainer.getMappedPort(5432)
        )

        initDatabase(topicNames, dbConfig)
        "Vi kan skrive testdata til db" {
            transaction {
                writeBatchToDb(TestData.dataRows.asSequence())
            }
        }

        "Verifiser spørringer på test data" - {
            "Vi kan hente id til periode A via identitetsnummer" {
                val perioder = ExposedDatabaseQuerySupport.hentPerioder(Identitetsnummer(TestData.periode_a_startet.identitetsnummer))
                perioder.size shouldBe 1
                perioder.first() shouldBe TestData.periode_a_startet.id
            }
            "Vi kan hente rader for periode A via periodeId" {
                val rader = ExposedDatabaseQuerySupport.hentRaderForPeriode(TestData.periode_a_startet.id)
                rader.size shouldBe 3
            }
        }
    }
})
