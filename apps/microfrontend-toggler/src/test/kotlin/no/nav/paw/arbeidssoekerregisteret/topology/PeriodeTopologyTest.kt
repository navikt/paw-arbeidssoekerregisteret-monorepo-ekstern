package no.nav.paw.arbeidssoekerregisteret.topology

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import no.nav.paw.arbeidssoekerregisteret.config.AppConfig
import no.nav.paw.arbeidssoekerregisteret.config.buildPeriodeInfoSerde
import no.nav.paw.arbeidssoekerregisteret.config.buildToggleSerde
import no.nav.paw.arbeidssoekerregisteret.context.ConfigContext
import no.nav.paw.arbeidssoekerregisteret.context.LoggingContext
import no.nav.paw.arbeidssoekerregisteret.model.PeriodeInfo
import no.nav.paw.arbeidssoekerregisteret.model.Sensitivitet
import no.nav.paw.arbeidssoekerregisteret.model.Toggle
import no.nav.paw.arbeidssoekerregisteret.model.ToggleAction
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.state.Stores
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant

class PeriodeTopologyTest : FreeSpec({

    with(TestContext()) {
        "Testsuite for toggling av microfrontends basert på arbeidssøkerperiode" - {
            val identitetsnummer = "01017012345"
            val arbeidsoekerId = 1234L
            val avsluttetTidspunkt = Instant.now()
            val startetTidspunkt = avsluttetTidspunkt.minus(Duration.ofDays(10))
            val avsluttetGammeltTidspunkt = Instant.now().minus(Duration.ofDays(22))
            val startetGammeltTidspunkt = avsluttetGammeltTidspunkt.minus(Duration.ofDays(10))
            val startetPeriode = buildPeriode(
                identitetsnummer = identitetsnummer,
                startet = startetTidspunkt
            )
            val avsluttetPeriode = buildPeriode(
                identitetsnummer = identitetsnummer,
                startet = startetTidspunkt,
                avsluttet = avsluttetTidspunkt
            )
            val startetGammelPeriode = buildPeriode(
                identitetsnummer = identitetsnummer,
                startet = startetGammeltTidspunkt
            )
            val avsluttetGammelPeriode = buildPeriode(
                identitetsnummer = identitetsnummer,
                startet = startetGammeltTidspunkt,
                avsluttet = avsluttetGammeltTidspunkt
            )
            val key = 9876L
            every { kafkaKeysClientMock.hentKafkaKeys(identitetsnummer) } returns KafkaKeysResponse(
                arbeidsoekerId,
                key
            )

            "Skal aktivere begge microfrontends ved start av periode" {
                periodeTopic.pipeInput(key, startetPeriode)

                microfrontendTopic.isEmpty shouldBe false
                val keyValueList = microfrontendTopic.readKeyValuesToList()
                keyValueList.size shouldBe 2

                val minSideKeyValue = keyValueList.first()
                val behovsvurderingKeyValue = keyValueList.last()

                minSideKeyValue.key shouldBe arbeidsoekerId
                with(minSideKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.ENABLE
                    ident shouldBe startetPeriode.identitetsnummer
                    microfrontendId shouldBe appConfig.microfrontends.aiaMinSide
                    sensitivitet shouldBe Sensitivitet.HIGH
                    initialedBy shouldBe "paw"
                }

                behovsvurderingKeyValue.key shouldBe arbeidsoekerId
                with(behovsvurderingKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.ENABLE
                    ident shouldBe startetPeriode.identitetsnummer
                    microfrontendId shouldBe appConfig.microfrontends.aiaBehovsvurdering
                    sensitivitet shouldBe Sensitivitet.HIGH
                    initialedBy shouldBe "paw"
                }

                periodeStateStore.size() shouldBe 1
                with(periodeStateStore.get(arbeidsoekerId).shouldBeInstanceOf<PeriodeInfo>()) {
                    id shouldBe startetPeriode.id
                    identitetsnummer shouldBe startetPeriode.identitetsnummer
                    arbeidssoekerId shouldBe arbeidsoekerId
                    startet shouldBe startetPeriode.startet.tidspunkt
                    avsluttet shouldBe null
                }
            }

            "Skal deaktivere aia-behovsvurdering microfrontend ved avslutting av periode" {
                periodeTopic.pipeInput(key, avsluttetPeriode)

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

                periodeStateStore.size() shouldBe 1
                with(periodeStateStore.get(arbeidsoekerId).shouldBeInstanceOf<PeriodeInfo>()) {
                    id shouldBe avsluttetPeriode.id
                    identitetsnummer shouldBe avsluttetPeriode.identitetsnummer
                    arbeidssoekerId shouldBe arbeidsoekerId
                    startet shouldBe avsluttetPeriode.startet.tidspunkt
                    avsluttet shouldBe avsluttetPeriode.avsluttet.tidspunkt
                }
            }

            "Skal deaktivere aia-min-side microfrontend 21 dager etter avslutting av periode" {
                testDriver.advanceWallClockTime(Duration.ofDays(22))

                microfrontendTopic.isEmpty shouldBe false
                val keyValueList = microfrontendTopic.readKeyValuesToList()
                keyValueList.size shouldBe 1

                val behovsvurderingKeyValue = keyValueList.last()

                behovsvurderingKeyValue.key shouldBe arbeidsoekerId
                with(behovsvurderingKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.DISABLE
                    ident shouldBe avsluttetPeriode.identitetsnummer
                    microfrontendId shouldBe appConfig.microfrontends.aiaMinSide
                    sensitivitet shouldBe null
                    initialedBy shouldBe "paw"
                }

                periodeStateStore.size() shouldBe 0
            }

            "Skal aktivere begge microfrontends ved start av gammel periode" {
                periodeTopic.pipeInput(key, startetGammelPeriode)

                microfrontendTopic.isEmpty shouldBe false
                val keyValueList = microfrontendTopic.readKeyValuesToList()
                keyValueList.size shouldBe 2

                val minSideKeyValue = keyValueList.first()
                val behovsvurderingKeyValue = keyValueList.last()

                minSideKeyValue.key shouldBe arbeidsoekerId
                with(minSideKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.ENABLE
                    ident shouldBe startetGammelPeriode.identitetsnummer
                    microfrontendId shouldBe appConfig.microfrontends.aiaMinSide
                    sensitivitet shouldBe Sensitivitet.HIGH
                    initialedBy shouldBe "paw"
                }

                behovsvurderingKeyValue.key shouldBe arbeidsoekerId
                with(behovsvurderingKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.ENABLE
                    ident shouldBe startetGammelPeriode.identitetsnummer
                    microfrontendId shouldBe appConfig.microfrontends.aiaBehovsvurdering
                    sensitivitet shouldBe Sensitivitet.HIGH
                    initialedBy shouldBe "paw"
                }

                periodeStateStore.size() shouldBe 1
                with(periodeStateStore.get(arbeidsoekerId).shouldBeInstanceOf<PeriodeInfo>()) {
                    id shouldBe startetGammelPeriode.id
                    identitetsnummer shouldBe startetGammelPeriode.identitetsnummer
                    arbeidssoekerId shouldBe arbeidsoekerId
                    startet shouldBe startetGammelPeriode.startet.tidspunkt
                    avsluttet shouldBe null
                }
            }

            "Skal deaktivere begge microfrontend ved avslutting av gammel periode" {
                periodeTopic.pipeInput(key, avsluttetGammelPeriode)

                microfrontendTopic.isEmpty shouldBe false
                val keyValueList = microfrontendTopic.readKeyValuesToList()
                keyValueList.size shouldBe 2

                val minSideKeyValue = keyValueList.first()
                val behovsvurderingKeyValue = keyValueList.last()

                minSideKeyValue.key shouldBe arbeidsoekerId
                with(minSideKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.DISABLE
                    ident shouldBe startetGammelPeriode.identitetsnummer
                    microfrontendId shouldBe appConfig.microfrontends.aiaMinSide
                    sensitivitet shouldBe null
                    initialedBy shouldBe "paw"
                }

                behovsvurderingKeyValue.key shouldBe arbeidsoekerId
                with(behovsvurderingKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.DISABLE
                    ident shouldBe avsluttetGammelPeriode.identitetsnummer
                    microfrontendId shouldBe appConfig.microfrontends.aiaBehovsvurdering
                    sensitivitet shouldBe null
                    initialedBy shouldBe "paw"
                }

                periodeStateStore.size() shouldBe 0
            }
        }
    }
}) {
    private class TestContext {

        val appConfig = loadNaisOrLocalConfiguration<AppConfig>(TEST_APPLICATION_CONFIG_FILE_NAME)
        val logger = LoggerFactory.getLogger("TestApplication")
        val auditLogger = LoggerFactory.getLogger("TestAudit")
        val meterRegistry = SimpleMeterRegistry()
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
                            meterRegistry,
                            kafkaKeysClientMock::hentKafkaKeys
                        )
                    }.build()
                }
            }.let { TopologyTestDriver(it, kafkaStreamProperties) }

        val periodeStateStore =
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
}