package no.nav.paw.arbeidssoekerregisteret.topology

import io.confluent.kafka.schemaregistry.testutil.MockSchemaRegistry
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import no.nav.common.types.identer.AktorId
import no.nav.paw.arbeidssoekerregisteret.model.Beriket14aVedtak
import no.nav.paw.arbeidssoekerregisteret.model.Siste14aVedtak
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.state.KeyValueStore
import java.time.Instant
import java.util.*

const val TEST_APPLICATION_CONFIG_FILE_NAME = "test_application_configuration.toml"

val kafkaStreamProperties = Properties().apply {
    this[StreamsConfig.APPLICATION_ID_CONFIG] = "test-kafka-streams"
    this[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = "dummy:1234"
    this[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG] = Serdes.Long().javaClass
    this[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG] = SpecificAvroSerde<SpecificRecord>().javaClass
    this[KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS] = "true"
    this[KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG] = "mock://$SCHEMA_REGISTRY_SCOPE"
}

const val SCHEMA_REGISTRY_SCOPE = "test-registry"

fun <T : SpecificRecord> buildAvroSerde(): Serde<T> {
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

class KafkaKeysClientMock {
    fun hentKafkaKeys(identitetsnummer: String): KafkaKeysResponse? {
        return null
    }
}

fun <K, V> KeyValueStore<K, V>.size(): Int {
    var count = 0
    for (keyValue in all()) {
        count++
    }
    return count
}

fun buildPeriode(
    id: UUID = UUID.randomUUID(),
    identitetsnummer: String,
    startet: Instant = Instant.now(),
    avsluttet: Instant? = null
) = Periode(
    id,
    identitetsnummer,
    Metadata(
        startet,
        Bruker(no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType.SLUTTBRUKER, identitetsnummer),
        "junit",
        "tester",
        null
    ),
    avsluttet?.let {
        Metadata(
            avsluttet,
            Bruker(no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType.SLUTTBRUKER, identitetsnummer),
            "junit",
            "tester",
            null
        )
    }
)

fun buildSiste14aVedtak(
    aktorId: String,
    fattetDato: Instant
) = Siste14aVedtak(
    AktorId(aktorId),
    "STANDARD_INNSATS",
    "SKAFFE_ARBEID",
    fattetDato,
    false
)

fun buildBeriket14aVedtak(
    aktorId: String,
    arbeidsoekerId: Long,
    fattetDato: Instant
) = Beriket14aVedtak(
    aktorId,
    arbeidsoekerId,
    "STANDARD_INNSATS",
    "SKAFFE_ARBEID",
    fattetDato,
    false
)
