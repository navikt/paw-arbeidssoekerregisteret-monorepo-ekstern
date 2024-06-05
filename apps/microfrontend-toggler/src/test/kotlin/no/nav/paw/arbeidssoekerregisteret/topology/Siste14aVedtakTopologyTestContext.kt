package no.nav.paw.arbeidssoekerregisteret.topology

import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.mockk.mockk
import no.nav.common.types.identer.AktorId
import no.nav.paw.arbeidssoekerregisteret.config.AppConfig
import no.nav.paw.arbeidssoekerregisteret.config.buildPeriodeInfoSerde
import no.nav.paw.arbeidssoekerregisteret.config.buildSiste14aVedtakSerde
import no.nav.paw.arbeidssoekerregisteret.config.buildToggleSerde
import no.nav.paw.arbeidssoekerregisteret.context.ConfigContext
import no.nav.paw.arbeidssoekerregisteret.context.LoggingContext
import no.nav.paw.arbeidssoekerregisteret.model.PeriodeInfo
import no.nav.paw.arbeidssoekerregisteret.model.Siste14aVedtak
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.state.Stores
import org.slf4j.LoggerFactory
import java.time.Instant

class Siste14aVedtakTopologyTestContext {

    val appConfig = loadNaisOrLocalConfiguration<AppConfig>(TEST_APPLICATION_CONFIG_FILE_NAME)
    val logger = LoggerFactory.getLogger("TestApplication")
    val auditLogger = LoggerFactory.getLogger("TestAudit")
    val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val periodeInfoSerde = buildPeriodeInfoSerde()
    val siste14aVedtakSerde = buildSiste14aVedtakSerde()
    val toggleSerde = buildToggleSerde()
    val kafkaKeysClientMock = mockk<KafkaKeysClientMock>()
    val pdlClientMock = mockk<PdlClientMock>()

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
                    buildSiste14aVedtakTopology(
                        meterRegistry,
                        kafkaKeysClientMock::hentKafkaKeys,
                        pdlClientMock::hentFolkeregisterIdent
                    )
                }.build()
            }
        }.let { TopologyTestDriver(it, kafkaStreamProperties) }


    val periodeKeyValueStore =
        testDriver.getKeyValueStore<Long, PeriodeInfo>(appConfig.kafkaStreams.periodeStoreName)

    val siste14aVedtakTopic = testDriver.createInputTopic(
        appConfig.kafkaStreams.siste14aVedtakTopic,
        Serdes.String().serializer(),
        siste14aVedtakSerde.serializer()
    )

    val microfrontendTopic = testDriver.createOutputTopic(
        appConfig.kafkaStreams.microfrontendTopic,
        Serdes.Long().deserializer(),
        toggleSerde.deserializer()
    )
}


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
