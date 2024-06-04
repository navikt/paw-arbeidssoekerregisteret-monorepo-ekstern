package no.nav.paw.arbeidssoekerregisteret.topology

import io.mockk.mockk
import no.nav.paw.arbeidssoekerregisteret.config.AppConfig
import no.nav.paw.arbeidssoekerregisteret.config.buildPeriodeInfoSerde
import no.nav.paw.arbeidssoekerregisteret.config.buildToggleSerde
import no.nav.paw.arbeidssoekerregisteret.context.ConfigContext
import no.nav.paw.arbeidssoekerregisteret.context.LoggingContext
import no.nav.paw.arbeidssoekerregisteret.model.PeriodeInfo
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.state.Stores
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

class PeriodeTopologyTestContext {

    val appConfig = loadNaisOrLocalConfiguration<AppConfig>(TEST_APPLICATION_CONFIG_FILE_NAME)
    val logger = LoggerFactory.getLogger("TestApplication")
    val auditLogger = LoggerFactory.getLogger("TestAudit")
    val periodeSerde = buildAvroSerde<Periode>()
    val periodeInfoSerde = buildPeriodeInfoSerde()
    val toggleSerde = buildToggleSerde()
    val kafkaKeysClientMock = mockk<KafkaKeysClientMock>()

    val testDriver =
        with(ConfigContext(appConfig)) {
            with(LoggingContext(logger, auditLogger)) {
                StreamsBuilder().apply {
                    addStateStore(
                        Stores.keyValueStoreBuilder(
                            Stores.inMemoryKeyValueStore(appConfig.kafkaStreams.periodeStoreName),
                            Serdes.Long(),
                            periodeInfoSerde
                        )
                    )
                    buildPeriodeTopology(
                        kafkaKeysClientMock::hentKafkaKeys
                    )
                }.build()
            }
        }.let { TopologyTestDriver(it, kafkaStreamProperties) }

    val periodeKeyValueStore =
        testDriver.getKeyValueStore<Long, PeriodeInfo>(appConfig.kafkaStreams.periodeStoreName)

    val periodeTopic = testDriver.createInputTopic(
        appConfig.kafkaStreams.periodeTopic,
        Serdes.Long().serializer(),
        periodeSerde.serializer()
    )

    val microfrontendTopic = testDriver.createOutputTopic(
        appConfig.kafkaStreams.microfrontendTopic,
        Serdes.Long().deserializer(),
        toggleSerde.deserializer()
    )
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
        "tester"
    ),
    avsluttet?.let {
        Metadata(
            avsluttet,
            Bruker(no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType.SLUTTBRUKER, identitetsnummer),
            "junit",
            "tester"
        )
    }
)
