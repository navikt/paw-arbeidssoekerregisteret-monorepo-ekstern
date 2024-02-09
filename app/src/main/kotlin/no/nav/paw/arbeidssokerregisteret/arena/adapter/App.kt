package no.nav.paw.arbeidssokerregisteret.arena.adapter

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import no.nav.paw.arbeidssokerregisteret.arena.adapter.config.ApplicationConfig
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import no.nav.paw.arbeidssokerregisteret.api.v3.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.arena.adapter.config.Topics
import no.nav.paw.arbeidssokerregisteret.arena.v3.ArenaArbeidssokerregisterTilstand
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.config.kafka.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.config.kafka.streams.KafkaStreamsFactory
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.utils.Time
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.JoinWindows
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.state.internals.KeyValueStoreBuilder
import org.apache.kafka.streams.state.internals.RocksDBKeyValueBytesStoreSupplier
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.*

val logger = LoggerFactory.getLogger("App")
fun main() {
    val kafkaStreamsConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG_WITH_SCHEME_REG)
    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>("application.toml")
    val applicationIdSuffix = kafkaStreamsConfig.applicationIdPrefix
        ?: throw RuntimeException("applicationIdPrefix is not set")
    val (topics) = applicationConfig

    val kafkaStreamsFactory = KafkaStreamsFactory(applicationIdSuffix, kafkaStreamsConfig)

    val periodeSerde = kafkaStreamsFactory.createSpecificAvroSerde<Periode>()
    val opplysningerOmArbeidssoekerSerde = kafkaStreamsFactory.createSpecificAvroSerde<OpplysningerOmArbeidssoeker>()
    val profileringSerde = kafkaStreamsFactory.createSpecificAvroSerde<Profilering>()
    val arenaArbeidssokerregisterTilstandSerde =
        kafkaStreamsFactory.createSpecificAvroSerde<ArenaArbeidssokerregisterTilstand>()

    val stateStoreName = "periodeStateStore"
    val builder = StreamsBuilder()
        .addStateStore(
            KeyValueStoreBuilder(
                RocksDBKeyValueBytesStoreSupplier(stateStoreName, false),
                Serdes.Long(),
                periodeSerde,
                Time.SYSTEM
            )
        )
    val topology: Topology = buildTopology(
        builder,
        topics,
        stateStoreName,
        periodeSerde,
        opplysningerOmArbeidssoekerSerde,
        profileringSerde,
        arenaArbeidssokerregisterTilstandSerde
    )
    val kafkaStreams = KafkaStreams(topology, kafkaStreamsFactory.properties)

    kafkaStreams.setUncaughtExceptionHandler { throwable ->
        logger.error("Uventet feil", throwable)
        StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_APPLICATION
    }

    kafkaStreams.start()

    Runtime.getRuntime().addShutdownHook(Thread(kafkaStreams::close))
}

fun buildTopology(
    builder: StreamsBuilder,
    topics: Topics,
    stateStoreName: String,
    periodeSerde: SpecificAvroSerde<Periode>,
    opplysningerOmArbeidssoekerSerde: SpecificAvroSerde<OpplysningerOmArbeidssoeker>,
    profileringSerde: SpecificAvroSerde<Profilering>,
    arenaArbeidssokerregisterTilstandSerde: SpecificAvroSerde<ArenaArbeidssokerregisterTilstand>
): Topology {
    builder
        .stream(topics.arbeidssokerperioder, Consumed.with(Serdes.Long(), periodeSerde))
        .saveToStore(stateStoreName)


    val opplysningerOmArbeidssoekerStream: KStream<Long, OpplysningerOmArbeidssoeker> = builder.stream(
        topics.opplysningerOmArbeidssoeker,
        Consumed.with(Serdes.Long(), opplysningerOmArbeidssoekerSerde)
    )

    val profileringStream: KStream<Long, Profilering> =
        builder.stream(topics.profilering, Consumed.with(Serdes.Long(), profileringSerde))

    val joinWindow = JoinWindows.ofTimeDifferenceWithNoGrace(
        Duration.ofMinutes(5)
    )

    opplysningerOmArbeidssoekerStream
        .join(
            profileringStream,
            { opplysninger, profilering -> opplysninger to profilering },
            joinWindow
        ).loadAndMap(stateStoreName)
        .peek { _, value ->
            logger.info("Sending $value to arena")
        }
        .to(topics.arena, Produced.with(Serdes.Long(), arenaArbeidssokerregisterTilstandSerde))

    val topology: Topology = builder.build()
    return topology
}

val ArenaArbeidssokerregisterTilstand.isValid: Boolean
    get() {
        val periodeId = periode.id
        val opplysningerPeriodeId = opplysningerOmArbeidssoeker.periodeId
        val profileringPeriodeId = profilering.periodeId
        val profileringOpplysningsId = profilering.opplysningerOmArbeidssokerId
        val validMatch = periodeId != opplysningerPeriodeId || periodeId != profileringPeriodeId ||
                profileringOpplysningsId != opplysningerOmArbeidssoeker.id
        return validMatch
    }

fun ArenaArbeidssokerregisterTilstand.info(): String =
    "periodeId=${periode.id}, opplysningsId=${opplysningerOmArbeidssoeker.id}, profilering=${profilering.id}," +
            " opplysninger.periodeId=${opplysningerOmArbeidssoeker.periodeId}, profilering.periodeId=${profilering.periodeId}," +
            "profilering.opplysningsId=${profilering.opplysningerOmArbeidssokerId}"

val testProfilering = Profilering(
    UUID.randomUUID(),
    UUID.randomUUID(),
    UUID.randomUUID(),
    Metadata(
        Instant.now(),
        Bruker(
            BrukerType.UKJENT_VERDI,
            "12345678911"
        ),
        "test",
        "test"
    ),
    ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING,
    true,
    30
)