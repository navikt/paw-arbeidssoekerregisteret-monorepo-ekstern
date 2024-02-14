package no.nav.arbeidssokerregisteret.arena.adapter

import io.confluent.kafka.schemaregistry.testutil.MockSchemaRegistry
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v3.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.arena.adapter.config.Topics
import no.nav.paw.arbeidssokerregisteret.arena.adapter.topology
import no.nav.paw.arbeidssokerregisteret.arena.helpers.v3.TopicsJoin
import no.nav.paw.arbeidssokerregisteret.arena.v3.ArenaArbeidssokerregisterTilstand
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.config.kafka.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.config.kafka.streams.KafkaStreamsFactory
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.utils.Time
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.state.internals.InMemoryKeyValueBytesStoreSupplier
import org.apache.kafka.streams.state.internals.KeyValueStoreBuilder

class TopologyTest : FreeSpec({
    "Når vi har sendt, periode, opplysninger og profilering skal vi få noe ut på arena topic" - {
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
                arenaArbeidssokerregisterTilstandSerde = createAvroSerde()
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
        TestData.perioder.forEach { (_, periode) ->
            periodeTopic.pipeInput(5L, periode)
        }
        TestData.opplysningerOmArbeidssoeker.forEach { (_, opplysning) ->
            opplysningerTopic.pipeInput(5L, opplysning)
        }
        TestData.profilering.forEach { (_, profilering) ->
            profileringsTopic.pipeInput(5L, profilering)
        }
        "Vi skal få ut en samlet melding" {
            arenaTopic.isEmpty shouldBe false
            val kv = arenaTopic.readKeyValue()
            kv.key shouldBe 5L
            kv.value.profilering.periodeId shouldBe TestData.perioder.first().second.id
        }
    }
})

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