package no.nav.arbeidssokerregisteret.arena.adapter

import io.confluent.kafka.schemaregistry.testutil.MockSchemaRegistry
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.arena.adapter.config.ArenaAdapterTopics
import no.nav.paw.arbeidssokerregisteret.arena.adapter.config.Topics
import no.nav.paw.arbeidssokerregisteret.arena.adapter.forsinkelseSerde
import no.nav.paw.arbeidssokerregisteret.arena.adapter.topology
import no.nav.paw.arbeidssokerregisteret.arena.helpers.v4.TopicsJoin
import no.nav.paw.arbeidssokerregisteret.arena.v8.ArenaArbeidssokerregisterTilstand
import no.nav.paw.arbeidssokerregisteret.standardTopicNames
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.kafka.config.KAFKA_STREAMS_CONFIG_WITH_SCHEME_REG
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.factory.KafkaStreamsFactory
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
import java.time.Instant
import java.util.*


data class TestScope(
    val periodeTopic: TestInputTopic<Long, Periode>,
    val opplysningerTopic: TestInputTopic<Long, OpplysningerOmArbeidssoeker>,
    val profileringsTopic: TestInputTopic<Long, Profilering>,
    val bekreftelseTopic: TestInputTopic<Long, Bekreftelse>,
    val arenaTopic: TestOutputTopic<Long, ArenaArbeidssokerregisterTilstand>,
    val joinStore: KeyValueStore<UUID, TopicsJoin>,
    val topologyTestDriver: TopologyTestDriver,
    val bekreftelseStore: KeyValueStore<UUID, Bekreftelse>,
)

fun testScope(initialWallClockTime: Instant = Instant.now()): TestScope {
    val topics = Topics(
        arenaTopics = ArenaAdapterTopics("arena"),
        standardTopics = standardTopicNames(currentRuntimeEnvironment)
    )
    val periodeSerde = createAvroSerde<Periode>()
    val tempArenaArbeidssokerregisterTilstandSerde = createAvroSerde<TopicsJoin>()
    val bekreftelseSerde = createAvroSerde<Bekreftelse>()
    val stateStoreName = "stateStore"
    val ventendeStateStoreName = "ventendeStateStore"
    val bekreftelseStoreName = "bekreftelse"
    val streamBuilder = StreamsBuilder()
        .addStateStore(
            KeyValueStoreBuilder(
                InMemoryKeyValueBytesStoreSupplier(stateStoreName),
                Serdes.UUID(),
                tempArenaArbeidssokerregisterTilstandSerde,
                Time.SYSTEM
            )
        )
        .addStateStore(
            KeyValueStoreBuilder(
                InMemoryKeyValueBytesStoreSupplier(ventendeStateStoreName),
                Serdes.UUID(),
                forsinkelseSerde,
                Time.SYSTEM
            )
        )
        .addStateStore(
            KeyValueStoreBuilder(
                InMemoryKeyValueBytesStoreSupplier(bekreftelseStoreName),
                Serdes.UUID(),
                bekreftelseSerde,
                Time.SYSTEM
            )
        )

    val kafkaConfig: KafkaConfig = loadNaisOrLocalConfiguration(KAFKA_STREAMS_CONFIG_WITH_SCHEME_REG)
    val kafkaStreamsFactory = KafkaStreamsFactory("test", kafkaConfig)
        .withDefaultKeySerde(Serdes.Long()::class)
        .withDefaultValueSerde(SpecificAvroSerde::class)

    val testDriver = TopologyTestDriver(
        topology(
            builder = streamBuilder,
            topics = topics,
            stateStoreName = stateStoreName,
            periodeSerde = periodeSerde,
            profileringSerde = createAvroSerde(),
            arenaArbeidssokerregisterTilstandSerde = createAvroSerde(),
            ventendePeriodeStateStoreName = ventendeStateStoreName,
            bekreftelseStateStoreName = bekreftelseStoreName,
            bekreftelseSerde = bekreftelseSerde
        ),
        kafkaStreamsFactory.properties,
        initialWallClockTime
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
    val bekreftelseTopic = testDriver.createInputTopic(
        topics.bekreftelse,
        Serdes.Long().serializer(),
        bekreftelseSerde.serializer()
    )
    return TestScope(
        periodeTopic = periodeTopic,
        opplysningerTopic = opplysningerTopic,
        profileringsTopic = profileringsTopic,
        arenaTopic = arenaTopic,
        joinStore = testDriver.getKeyValueStore(stateStoreName),
        topologyTestDriver = testDriver,
        bekreftelseTopic = bekreftelseTopic,
        bekreftelseStore = testDriver.getKeyValueStore(bekreftelseStoreName)
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