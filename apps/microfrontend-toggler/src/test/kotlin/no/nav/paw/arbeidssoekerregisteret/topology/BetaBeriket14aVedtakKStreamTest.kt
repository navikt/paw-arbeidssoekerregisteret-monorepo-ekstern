package no.nav.paw.arbeidssoekerregisteret.topology

import io.kotest.core.annotation.Ignored
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import no.nav.paw.arbeidssoekerregisteret.config.AppConfig
import no.nav.paw.arbeidssoekerregisteret.config.buildBeriket14aVedtakSerde
import no.nav.paw.arbeidssoekerregisteret.config.buildPeriodeInfoSerde
import no.nav.paw.arbeidssoekerregisteret.config.buildToggleSerde
import no.nav.paw.arbeidssoekerregisteret.context.ConfigContext
import no.nav.paw.arbeidssoekerregisteret.context.LoggingContext
import no.nav.paw.arbeidssoekerregisteret.model.PeriodeInfo
import no.nav.paw.arbeidssoekerregisteret.model.Toggle
import no.nav.paw.arbeidssoekerregisteret.model.ToggleAction
import no.nav.paw.arbeidssoekerregisteret.model.buildPeriodeInfo
import no.nav.paw.arbeidssoekerregisteret.topology.streams.buildBeriket14aVedtakKStream
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.state.Stores
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant

/**
 *         14a1       14a2       14a3
 * <--------|----------|----------|---->
 *                |--- p1 -->|
 *                |--- p2 ------------->
 *
 * 14a1: Skal ikke deaktivere aia-behovsvurdering når det ikke finnes periode ved vedtakstidspunkt
 * 14a2: Skal ikke deaktivere aia-behovsvurdering når det kun finnes en avsluttet periode ved vedtakstidspunkt
 * 14a3: Skal deaktivere aia-behovsvurdering når det finnes en aktiv periode ved vedtakstidspunkt
 *
 */
@Ignored
class BetaBeriket14aVedtakKStreamTest : FreeSpec({

    with(TestContext()) {
        "Testsuite for toggling av AIA-microfrontends basert på beriket 14a vedtak" - {
            val aktivKey = 23456L
            val identitetsnummer = "01017012345"
            val arbeidsoekerId = 1234L
            val aktorId = "12345"
            val periodeAvsluttetTidspunkt = Instant.now()
            val periodeStartTidspunkt = periodeAvsluttetTidspunkt.minus(Duration.ofDays(10))
            val startetPeriode = buildPeriode(identitetsnummer = identitetsnummer, startet = periodeStartTidspunkt)
            val avsluttetPeriode = buildPeriode(
                identitetsnummer = identitetsnummer,
                startet = periodeStartTidspunkt,
                avsluttet = periodeAvsluttetTidspunkt
            )
            val beriket14aVedtak =
                buildBeriket14aVedtak(aktorId, arbeidsoekerId, periodeStartTidspunkt.plus(Duration.ofDays(2)))
            every { kafkaKeysClientMock.hentKafkaKeys(aktorId) } returns KafkaKeysResponse(
                arbeidsoekerId,
                aktivKey
            )

            "Skal ikke deaktivere aia-behovsvurdering microfrontend om det ikke finnes noen periode tilhørende 14a vedtak" {
                beriket14aVedtakTopic.pipeInput(aktivKey, beriket14aVedtak)

                microfrontendTopic.isEmpty shouldBe true
                periodeKeyValueStore.size() shouldBe 0
            }

            "Skal ikke deaktivere aia-behovsvurdering microfrontend om det ikke finnes en aktiv periode tilhørende 14a vedtak" {
                periodeKeyValueStore.put(arbeidsoekerId, avsluttetPeriode.buildPeriodeInfo(arbeidsoekerId))

                beriket14aVedtakTopic.pipeInput(aktivKey, beriket14aVedtak)

                microfrontendTopic.isEmpty shouldBe true
                periodeKeyValueStore.size() shouldBe 1
            }

            "Skal deaktivere aia-behovsvurdering microfrontend om det finnes en aktiv periode tilhørende 14a vedtak" {
                periodeKeyValueStore.put(arbeidsoekerId, startetPeriode.buildPeriodeInfo(arbeidsoekerId))

                beriket14aVedtakTopic.pipeInput(aktivKey, beriket14aVedtak)

                microfrontendTopic.isEmpty shouldBe false
                val keyValueList = microfrontendTopic.readKeyValuesToList()
                keyValueList.size shouldBe 1

                val behovsvurderingKeyValue = keyValueList.last()

                behovsvurderingKeyValue.key shouldBe arbeidsoekerId
                with(behovsvurderingKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.DISABLE
                    ident shouldBe avsluttetPeriode.identitetsnummer
                    microfrontendId shouldBe appConfig.microfrontends.aiaBehovsvurdering
                    sensitivitet shouldBe null
                    initialedBy shouldBe "paw"
                }

                periodeKeyValueStore.size() shouldBe 1
            }
        }
    }
}) {
    private class TestContext {

        val appConfig = loadNaisOrLocalConfiguration<AppConfig>(BETA_TEST_APPLICATION_CONFIG_FILE_NAME)
        val logger: Logger = LoggerFactory.getLogger("TestApplication")
        val auditLogger: Logger = LoggerFactory.getLogger("TestAudit")
        val meterRegistry = SimpleMeterRegistry()
        val periodeInfoSerde = buildPeriodeInfoSerde()
        val beriket14aVedtakSerde = buildBeriket14aVedtakSerde()
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
                        buildBeriket14aVedtakKStream(
                            meterRegistry,
                            kafkaKeysClientMock::hentKafkaKeys
                        )
                    }.build()
                }
            }.let { TopologyTestDriver(it, kafkaStreamProperties) }


        val periodeKeyValueStore =
            testDriver.getKeyValueStore<Long, PeriodeInfo>(appConfig.kafkaStreams.periodeStoreName)

        val beriket14aVedtakTopic = testDriver.createInputTopic(
            appConfig.kafkaStreams.beriket14aVedtakTopic,
            Serdes.Long().serializer(),
            beriket14aVedtakSerde.serializer()
        )

        val microfrontendTopic = testDriver.createOutputTopic(
            appConfig.kafkaStreams.microfrontendTopic,
            Serdes.Long().deserializer(),
            toggleSerde.deserializer()
        )
    }
}