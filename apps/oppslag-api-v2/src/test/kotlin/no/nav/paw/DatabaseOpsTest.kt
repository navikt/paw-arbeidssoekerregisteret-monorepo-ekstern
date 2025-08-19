package no.nav.paw

import io.confluent.kafka.schemaregistry.testutil.MockSchemaRegistry
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.opentelemetry.api.trace.Span
import no.nav.paw.arbeidssokerregisteret.standardTopicNames
import no.nav.paw.config.env.Local
import no.nav.paw.database.config.DatabaseConfig
import no.nav.paw.model.Identitetsnummer
import no.nav.paw.oppslagapi.data.consumer.toRow
import no.nav.paw.oppslagapi.data.consumer.writeBatchToDb
import no.nav.paw.oppslagapi.data.query.ExposedDatabaseQuerySupport
import no.nav.paw.oppslagapi.initDatabase
import no.nav.paw.oppslagsapi.periode
import no.nav.paw.test.data.periode.createOpplysninger
import no.nav.paw.test.data.periode.createProfilering
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.Serde
import org.jetbrains.exposed.sql.transactions.transaction
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.utility.DockerImageName
import java.time.Duration.ofMinutes
import java.time.Instant

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

object TestData {
    private val metdataFactory = no.nav.paw.test.data.periode.MetadataFactory.create()
    val periode_a_startet = periode(identitetsnummer = Identitetsnummer("12345678901"), startet = Instant.now())
    val periode_a_opplysninger = createOpplysninger(periodeId = periode_a_startet.id, sendtInnAv = metdataFactory.build(tidspunkt = Instant.now()))
    val periode_a_avsluttet = periode(periodeId = periode_a_startet.id, identitetsnummer = Identitetsnummer(periode_a_startet.identitetsnummer), avsluttet = Instant.now() + ofMinutes(1), startet = periode_a_startet.startet.tidspunkt)
    val periode_b_startet = periode(identitetsnummer = Identitetsnummer("12345678902"), startet = Instant.now())
    val periode_b_opplysninger = createOpplysninger(periodeId = periode_b_startet.id, sendtInnAv = metdataFactory.build(tidspunkt = Instant.now()))
    val periode_b_avsluttet = periode(periodeId = periode_b_startet.id, identitetsnummer = Identitetsnummer(periode_b_startet.identitetsnummer), avsluttet = Instant.now() + ofMinutes(1), startet = periode_b_startet.startet.tidspunkt)
    val periode_c_startet = periode(identitetsnummer = Identitetsnummer("12345678902"), startet = Instant.now())
    val periode_c_opplysninger = createOpplysninger(periodeId = periode_c_startet.id, sendtInnAv = metdataFactory.build(tidspunkt = Instant.now()))
    val periode_c_profilering = createProfilering(periodeId = periode_c_startet.id, opplysningerId = periode_c_opplysninger.id, sendtInnAv = metdataFactory.build(tidspunkt = Instant.now()))
    val periode_c_avsluttet = periode(periodeId = periode_c_startet.id, identitetsnummer = Identitetsnummer(periode_c_startet.identitetsnummer), avsluttet = Instant.now() + ofMinutes(1), startet = periode_c_startet.startet.tidspunkt)

    val data = listOf(
        periode_a_startet,
        periode_a_opplysninger,
        periode_a_avsluttet,
        periode_b_startet,
        periode_b_opplysninger,
        periode_b_avsluttet,
        periode_c_startet,
        periode_c_opplysninger,
        periode_c_profilering,
        periode_c_avsluttet
    )

    val dataRows get() =
        data
            .map(SpecificRecord::toConsumerRecord)
            .map{ it.toRow(serde.deserializer()) to Span.current()}
}

fun SpecificRecord.toConsumerRecord(): ConsumerRecord<Long, ByteArray> {
    return ConsumerRecord(
        "topic",
        0,
        0L,
        0L,
        serde.serializer().serialize("topic", this)
    )
}

const val SCHEMA_REGISTRY_SCOPE = "juni-registry"

fun <T : SpecificRecord> opprettSerde(): Serde<T> {
    val schemaRegistryClient = MockSchemaRegistry.getClientForScope(SCHEMA_REGISTRY_SCOPE)
    val serde: Serde<T> = SpecificAvroSerde(schemaRegistryClient)
    serde.configure(
        mapOf(
            KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS to "true",
            KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG to "mock://$SCHEMA_REGISTRY_SCOPE"
        ),
        false
    )
    return serde
}
