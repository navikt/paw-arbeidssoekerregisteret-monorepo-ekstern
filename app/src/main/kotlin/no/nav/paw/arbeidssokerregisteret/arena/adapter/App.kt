package no.nav.paw.arbeidssokerregisteret.arena.adapter

import no.nav.paw.arbeidssokerregisteret.arena.adapter.config.ApplicationConfig
import no.nav.paw.arbeidssokerregisteret.arena.v3.ArenaArbeidssokerregisterTilstand
import no.nav.paw.arbeidssokerregisteret.arena.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.arena.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.arena.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.arena.v3.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.arena.v1.Periode
import no.nav.paw.arbeidssokerregisteret.arena.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.arena.v1.ProfilertTil
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.config.kafka.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.config.kafka.streams.KafkaStreamsFactory
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.JoinWindows
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.KTable
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.kstream.StreamJoined
import org.apache.kafka.streams.kstream.ValueJoiner
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.*

fun main() {
    val logger = LoggerFactory.getLogger("App")

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

    val builder = StreamsBuilder()

    val periodeStream: KTable<Long, Periode> =
        builder
            .stream(topics.arbeidssokerperioder, Consumed.with(Serdes.Long(), periodeSerde))
            .toTable()

    val opplysningerOmArbeidssoekerStream: KStream<Long, OpplysningerOmArbeidssoeker> = builder.stream(
        topics.opplysningerOmArbeidssoeker,
        Consumed.with(Serdes.Long(), opplysningerOmArbeidssoekerSerde)
    )

    val profileringStream: KStream<Long, Profilering> =
        builder.stream(topics.profilering, Consumed.with(Serdes.Long(), profileringSerde))


    val joinWindow = JoinWindows.ofTimeDifferenceWithNoGrace(
        Duration.ofMinutes(5)
    )
    val joinErrorLogger = LoggerFactory.getLogger("join_error")
    opplysningerOmArbeidssoekerStream
        .join(
            profileringStream,
            { opplysninger, profilering -> opplysninger to profilering },
            joinWindow
        )
        .join(periodeStream) {
                (opplysninger, profilering), periode ->
                ArenaArbeidssokerregisterTilstand(
                    periode,
                    profilering,
                    opplysninger
                )
        }
        .peek { _, value ->
            logger.info("Sending $value to arena")
        }
        .peek { _, value ->
            val periodeId = value.periode.id
            val opplysningerPeriodeId = value.opplysningerOmArbeidssoeker.periodeId
            val profileringPeriodeId = value.profilering.periodeId
            val profileringOpplysningsId = value.profilering.opplysningerOmArbeidssokerId
            if (periodeId != opplysningerPeriodeId || periodeId != profileringPeriodeId ||
                profileringOpplysningsId != value.opplysningerOmArbeidssoeker.id) {
                joinErrorLogger.error(value.info())
            }
        }
        .to(topics.arena, Produced.with(Serdes.Long(), arenaArbeidssokerregisterTilstandSerde))



    val topology: Topology = builder.build()
    val kafkaStreams = KafkaStreams(topology, kafkaStreamsFactory.properties)

    kafkaStreams.setUncaughtExceptionHandler { throwable ->
        logger.error("Uventet feil", throwable)
        StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_APPLICATION
    }

    kafkaStreams.start()

    Runtime.getRuntime().addShutdownHook(Thread(kafkaStreams::close))
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