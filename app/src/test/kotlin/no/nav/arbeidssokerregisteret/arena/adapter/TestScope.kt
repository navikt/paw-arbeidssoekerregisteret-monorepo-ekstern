package no.nav.arbeidssokerregisteret.arena.adapter

import io.confluent.kafka.schemaregistry.testutil.MockSchemaRegistry
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.arena.adapter.config.Topics
import no.nav.paw.arbeidssokerregisteret.arena.adapter.topology
import no.nav.paw.arbeidssokerregisteret.arena.helpers.v4.TopicsJoin
import no.nav.paw.arbeidssokerregisteret.arena.v4.ArenaArbeidssokerregisterTilstand
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.config.kafka.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.config.kafka.streams.KafkaStreamsFactory
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.utils.Time
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.TestInputTopic
import org.apache.kafka.streams.TestOutputTopic
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.state.KeyValueStore
import org.apache.kafka.streams.state.internals.InMemoryKeyValueBytesStoreSupplier
import org.apache.kafka.streams.state.internals.KeyValueStoreBuilder


data class TestScope(
    val periodeTopic: TestInputTopic<Long, Periode>,
    val opplysningerTopic: TestInputTopic<Long, OpplysningerOmArbeidssoeker>,
    val profileringsTopic: TestInputTopic<Long, Profilering>,
    val arenaTopic: TestOutputTopic<Long, ArenaArbeidssokerregisterTilstand>,
    val joinStore: KeyValueStore<String, TopicsJoin>,
    val topologyTestDriver: TopologyTestDriver
)


fun testScope(): TestScope {
    val topics = Topics(
        opplysningerOmArbeidssoeker = "opplysninger",
        arbeidssokerperioder = "perioder",
        profilering = "profilering",
        arena = "arena"
    )
    val periodeSerde = createAvroSerde<Periode>()
    val tempArenaArbeidssokerregisterTilstandSerde = createAvroSerde<TopicsJoin>()
    val stateStoreName = "stateStore"
    val streamBuilder = StreamsBuilder()
        .addStateStore(
            KeyValueStoreBuilder(
                InMemoryKeyValueBytesStoreSupplier(stateStoreName),
                Serdes.String(),
                tempArenaArbeidssokerregisterTilstandSerde,
                Time.SYSTEM
            )
        )

    val kafkaConfig: KafkaConfig = loadNaisOrLocalConfiguration(KAFKA_CONFIG_WITH_SCHEME_REG)
    val kafkaStreamsFactory = KafkaStreamsFactory("test", kafkaConfig)
        .withDefaultKeySerde(Serdes.Long()::class)
        .withDefaultValueSerde(SpecificAvroSerde::class)

    val testDriver = TopologyTestDriver(
        topology(
            builder = streamBuilder,
            topics = topics,
            stateStoreName = stateStoreName,
            periodeSerde = periodeSerde,
            opplysningerOmArbeidssoekerSerde = createAvroSerde(),
            profileringSerde = createAvroSerde(),
            arenaArbeidssokerregisterTilstandSerde = createAvroSerde(),
            registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
        ),
        kafkaStreamsFactory.properties
    )
    val periodeTopic = testDriver.createInputTopic(
        topics.arbeidssokerperioder,
        Serdes.Long().serializer(),
        periodeSerde.serializer()
    )
    val opplysningerTopic = testDriver.createInputTopic(
        topics.opplysningerOmArbeidssoeker,
        Serdes.Long().serializer(),
        createAvroSerde<OpplysningerOmArbeidssoeker>().serializer(),
    )
    val profileringsTopic = testDriver.createInputTopic(
        topics.profilering,
        Serdes.Long().serializer(),
        createAvroSerde<Profilering>().serializer()
    )
    val arenaTopic = testDriver.createOutputTopic(
        topics.arena,
        Serdes.Long().deserializer(),
        createAvroSerde<ArenaArbeidssokerregisterTilstand>().deserializer()
    )
    return TestScope(
        periodeTopic = periodeTopic,
        opplysningerTopic = opplysningerTopic,
        profileringsTopic = profileringsTopic,
        arenaTopic = arenaTopic,
        joinStore = testDriver.getKeyValueStore(stateStoreName),
        topologyTestDriver = testDriver
    )
}

inline fun <reified T : SpecificRecord> createAvroSerde(): Serde<T> {
    val SCHEMA_REGISTRY_SCOPE = "mock"
    return SpecificAvroSerde<T>(MockSchemaRegistry.getClientForScope(SCHEMA_REGISTRY_SCOPE)).apply {
        configure(
            mapOf(
                KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS to "true",
                KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG to "mock://$SCHEMA_REGISTRY_SCOPE"
            ),
            false
        )
    }
}