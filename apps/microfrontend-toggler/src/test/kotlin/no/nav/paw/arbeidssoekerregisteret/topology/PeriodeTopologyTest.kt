package no.nav.paw.arbeidssoekerregisteret.topology

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
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
            val nyereIdentitetsnummer = "01017012345"
            val eldreIdentitetsnummer = "02017012345"
            val nyereArbeidssoekerId = nyereIdentitetsnummer.toLong()
            val eldreArbeidssoekerId = eldreIdentitetsnummer.toLong()
            val avsluttetNyereTidspunkt = Instant.now()
            val startetNyereTidspunkt = avsluttetNyereTidspunkt.minus(Duration.ofDays(10))
            val avsluttetEldreTidspunkt = Instant.now().minus(Duration.ofDays(22))
            val startetEldreTidspunkt = avsluttetEldreTidspunkt.minus(Duration.ofDays(10))
            val startetNyerePeriode = buildPeriode(
                identitetsnummer = nyereIdentitetsnummer,
                startet = startetNyereTidspunkt
            )
            val avsluttetNyerePeriode = buildPeriode(
                identitetsnummer = nyereIdentitetsnummer,
                startet = startetNyereTidspunkt,
                avsluttet = avsluttetNyereTidspunkt
            )
            val startetEldrePeriode = buildPeriode(
                identitetsnummer = eldreIdentitetsnummer,
                startet = startetEldreTidspunkt
            )
            val avsluttetEldrePeriode = buildPeriode(
                identitetsnummer = eldreIdentitetsnummer,
                startet = startetEldreTidspunkt,
                avsluttet = avsluttetEldreTidspunkt
            )
            val identitetsnummerSlot = slot<String>()
            every { kafkaKeysClientMock.hentKafkaKeys(capture(identitetsnummerSlot)) } answers {
                KafkaKeysResponse(
                    identitetsnummerSlot.captured.toLong(),
                    identitetsnummerSlot.captured.toLong()
                )
            }

            "Skal aktivere begge microfrontends ved start av nyere periode" {
                val key = nyereArbeidssoekerId
                periodeTopic.pipeInput(key, startetNyerePeriode)

                microfrontendTopic.isEmpty shouldBe false
                val keyValueList = microfrontendTopic.readKeyValuesToList()
                keyValueList.size shouldBe 2

                val minSideKeyValue = keyValueList.first()
                val behovsvurderingKeyValue = keyValueList.last()

                minSideKeyValue.key shouldBe nyereIdentitetsnummer.toLong()
                with(minSideKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.ENABLE
                    ident shouldBe startetNyerePeriode.identitetsnummer
                    microfrontendId shouldBe appConfig.microfrontends.aiaMinSide
                    sensitivitet shouldBe Sensitivitet.HIGH
                    initialedBy shouldBe "paw"
                }

                behovsvurderingKeyValue.key shouldBe nyereArbeidssoekerId
                with(behovsvurderingKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.ENABLE
                    ident shouldBe startetNyerePeriode.identitetsnummer
                    microfrontendId shouldBe appConfig.microfrontends.aiaBehovsvurdering
                    sensitivitet shouldBe Sensitivitet.HIGH
                    initialedBy shouldBe "paw"
                }

                periodeStateStore.size() shouldBe 1
                with(periodeStateStore.get(nyereArbeidssoekerId).shouldBeInstanceOf<PeriodeInfo>()) {
                    id shouldBe startetNyerePeriode.id
                    identitetsnummer shouldBe startetNyerePeriode.identitetsnummer
                    arbeidssoekerId shouldBe nyereArbeidssoekerId
                    startet shouldBe startetNyerePeriode.startet.tidspunkt
                    avsluttet shouldBe null
                }
            }

            "Skal deaktivere aia-behovsvurdering microfrontend ved avslutting av nyere periode" {
                val key = avsluttetNyerePeriode.identitetsnummer.toLong()
                periodeTopic.pipeInput(key, avsluttetNyerePeriode)

                microfrontendTopic.isEmpty shouldBe false
                val keyValueList = microfrontendTopic.readKeyValuesToList()
                keyValueList.size shouldBe 1

                val behovsvurderingKeyValue = keyValueList.last()

                behovsvurderingKeyValue.key shouldBe nyereArbeidssoekerId
                with(behovsvurderingKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.DISABLE
                    ident shouldBe avsluttetNyerePeriode.identitetsnummer
                    microfrontendId shouldBe appConfig.microfrontends.aiaBehovsvurdering
                    sensitivitet shouldBe null
                    initialedBy shouldBe "paw"
                }

                periodeStateStore.size() shouldBe 1
                with(periodeStateStore.get(nyereArbeidssoekerId).shouldBeInstanceOf<PeriodeInfo>()) {
                    id shouldBe avsluttetNyerePeriode.id
                    identitetsnummer shouldBe avsluttetNyerePeriode.identitetsnummer
                    arbeidssoekerId shouldBe nyereArbeidssoekerId
                    startet shouldBe avsluttetNyerePeriode.startet.tidspunkt
                    avsluttet shouldBe avsluttetNyerePeriode.avsluttet.tidspunkt
                }
            }

            "Skal deaktivere aia-min-side microfrontend 21 dager etter avslutting av nyere periode" {
                testDriver.advanceWallClockTime(Duration.ofDays(22))

                microfrontendTopic.isEmpty shouldBe false
                val keyValueList = microfrontendTopic.readKeyValuesToList()
                keyValueList.size shouldBe 1

                val behovsvurderingKeyValue = keyValueList.last()

                behovsvurderingKeyValue.key shouldBe nyereArbeidssoekerId
                with(behovsvurderingKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.DISABLE
                    ident shouldBe avsluttetNyerePeriode.identitetsnummer
                    microfrontendId shouldBe appConfig.microfrontends.aiaMinSide
                    sensitivitet shouldBe null
                    initialedBy shouldBe "paw"
                }

                periodeStateStore.size() shouldBe 0
            }

            "Skal aktivere begge microfrontends ved start av eldre periode" {
                val key = eldreArbeidssoekerId
                periodeTopic.pipeInput(key, startetEldrePeriode)

                microfrontendTopic.isEmpty shouldBe false
                val keyValueList = microfrontendTopic.readKeyValuesToList()
                keyValueList.size shouldBe 2

                val minSideKeyValue = keyValueList.first()
                val behovsvurderingKeyValue = keyValueList.last()

                minSideKeyValue.key shouldBe eldreArbeidssoekerId
                with(minSideKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.ENABLE
                    ident shouldBe startetEldrePeriode.identitetsnummer
                    microfrontendId shouldBe appConfig.microfrontends.aiaMinSide
                    sensitivitet shouldBe Sensitivitet.HIGH
                    initialedBy shouldBe "paw"
                }

                behovsvurderingKeyValue.key shouldBe eldreArbeidssoekerId
                with(behovsvurderingKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.ENABLE
                    ident shouldBe startetEldrePeriode.identitetsnummer
                    microfrontendId shouldBe appConfig.microfrontends.aiaBehovsvurdering
                    sensitivitet shouldBe Sensitivitet.HIGH
                    initialedBy shouldBe "paw"
                }

                periodeStateStore.size() shouldBe 1
                with(periodeStateStore.get(eldreArbeidssoekerId).shouldBeInstanceOf<PeriodeInfo>()) {
                    id shouldBe startetEldrePeriode.id
                    identitetsnummer shouldBe startetEldrePeriode.identitetsnummer
                    arbeidssoekerId shouldBe eldreArbeidssoekerId
                    startet shouldBe startetEldrePeriode.startet.tidspunkt
                    avsluttet shouldBe null
                }
            }

            "Skal deaktivere begge microfrontend ved avslutting av eldre periode" {
                val key = eldreArbeidssoekerId
                periodeTopic.pipeInput(key, avsluttetEldrePeriode)

                microfrontendTopic.isEmpty shouldBe false
                val keyValueList = microfrontendTopic.readKeyValuesToList()
                keyValueList.size shouldBe 2

                val minSideKeyValue = keyValueList.first()
                val behovsvurderingKeyValue = keyValueList.last()

                minSideKeyValue.key shouldBe eldreArbeidssoekerId
                with(minSideKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.DISABLE
                    ident shouldBe startetEldrePeriode.identitetsnummer
                    microfrontendId shouldBe appConfig.microfrontends.aiaMinSide
                    sensitivitet shouldBe null
                    initialedBy shouldBe "paw"
                }

                behovsvurderingKeyValue.key shouldBe eldreArbeidssoekerId
                with(behovsvurderingKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.DISABLE
                    ident shouldBe avsluttetEldrePeriode.identitetsnummer
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