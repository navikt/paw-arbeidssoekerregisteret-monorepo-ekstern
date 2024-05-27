package no.nav.paw.arbeidssoekerregisteret.topology

import io.confluent.kafka.schemaregistry.testutil.MockSchemaRegistry
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import no.nav.paw.arbeidssoekerregisteret.config.AppConfig
import no.nav.paw.arbeidssoekerregisteret.config.buildToggleSerde
import no.nav.paw.arbeidssoekerregisteret.config.buildToggleStateSerde
import no.nav.paw.arbeidssoekerregisteret.context.ConfigContext
import no.nav.paw.arbeidssoekerregisteret.context.LoggingContext
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.state.Stores
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

const val TEST_APPLICATION_CONFIG_FILE_NAME = "test_application_configuration.toml"
const val TEST_KAFKA_STREAMS_CONFIG_FILE_NAME = "test_kafka_streams_configuration.toml"

class TopologyTestContext {
    val appConfig = loadNaisOrLocalConfiguration<AppConfig>(TEST_APPLICATION_CONFIG_FILE_NAME)
    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(TEST_KAFKA_STREAMS_CONFIG_FILE_NAME)
    val logger = LoggerFactory.getLogger("no.nav.paw.test")
    val kafkaKeysClient = KafkaKeysClientMock()
    val keySerde = Serdes.Long()
    val periodeSerde = buildAvroSerde<Periode>()
    val toggleSerde = buildToggleSerde()
    val toggleStateSerde = buildToggleStateSerde()

    private val testDriver =
        with(ConfigContext(appConfig, kafkaConfig)) {
            with(LoggingContext(logger)) {
                StreamsBuilder().apply {
                    addStateStore(
                        Stores.keyValueStoreBuilder(
                            Stores.inMemoryKeyValueStore(appConfig.kafkaTopology.toggleStoreName),
                            keySerde,
                            toggleStateSerde
                        )
                    )
                    buildPeriodeTopology(kafkaKeysClient)
                }.build()
            }
        }.let { TopologyTestDriver(it, kafkaStreamProperties) }

    val periodeTopic = testDriver.createInputTopic(
        appConfig.kafkaTopology.periodeTopic,
        keySerde.serializer(),
        periodeSerde.serializer()
    )

    val microfrontendTopic = testDriver.createInputTopic(
        appConfig.kafkaTopology.microfrontendTopic,
        keySerde.serializer(),
        toggleSerde.serializer()
    )
}

const val SCHEMA_REGISTRY_SCOPE = "juni-registry"

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

val kafkaStreamProperties = Properties().apply {
    this[StreamsConfig.APPLICATION_ID_CONFIG] = "test"
    this[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = "dummy:1234"
    this[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG] = Serdes.Long().javaClass
    this[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG] = SpecificAvroSerde<SpecificRecord>().javaClass
    this[KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS] = "true"
    this[KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG] = "mock://$SCHEMA_REGISTRY_SCOPE"
}

class KafkaKeysClientMock : KafkaKeysClient {
    val map = ConcurrentHashMap<String, KafkaKeysResponse>()
    val sequence = AtomicLong(0)
    override suspend fun getIdAndKey(identitetsnummer: String): KafkaKeysResponse {
        return map.computeIfAbsent(identitetsnummer) {
            val id = sequence.getAndIncrement()
            KafkaKeysResponse(sequence.getAndIncrement(), id % 2)
        }
    }
}