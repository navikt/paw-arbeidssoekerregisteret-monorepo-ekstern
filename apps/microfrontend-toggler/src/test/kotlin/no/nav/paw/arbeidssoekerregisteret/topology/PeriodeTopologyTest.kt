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
import java.util.*

/**
 *                 -21d                  -10d        -5d         Nå
 * <----------------|----------------------|----------|----------|----->
 *    |--- p1 -->|
 *                                         |--- p2 -->|
 *
 * p1: Skal aktivere både aia-min-side og aia-behovsvurdering ved start og deaktivere begge ved avslutting
 * p2: Skal aktivere både aia-min-side og aia-behovsvurdering ved start og deaktivere kun aia-behovsvurdering
 *     ved deaktivering. Når det er gått > 21d siden avslutting skal aia-min-side deaktiveres.
 *
 */
class PeriodeTopologyTest : FreeSpec({

    with(TestContext()) {
        "Testsuite for toggling av microfrontends basert på arbeidssøkerperiode" - {
            val p1Id = UUID.randomUUID()
            val p1Identitetsnummer = "02017012345"
            val p1ArbeidssoekerId = p1Identitetsnummer.toLong()
            val p1AvsluttetTidspunkt = Instant.now().minus(Duration.ofDays(22))
            val p1StartetTidspunkt = p1AvsluttetTidspunkt.minus(Duration.ofDays(5))
            val p1StartetPeriode = buildPeriode(
                id = p1Id,
                identitetsnummer = p1Identitetsnummer,
                startet = p1StartetTidspunkt
            )
            val p1AvsluttetPeriode = buildPeriode(
                id = p1Id,
                identitetsnummer = p1Identitetsnummer,
                startet = p1StartetTidspunkt,
                avsluttet = p1AvsluttetTidspunkt
            )

            val p2Id = UUID.randomUUID()
            val p2Identitetsnummer = "01017012345"
            val p2ArbeidssoekerId = p2Identitetsnummer.toLong()
            val p2AvsluttetTidspunkt = Instant.now().minus(Duration.ofDays(5))
            val p2StartetTidspunkt = p2AvsluttetTidspunkt.minus(Duration.ofDays(10))
            val p2StartetPeriode = buildPeriode(
                id = p2Id,
                identitetsnummer = p2Identitetsnummer,
                startet = p2StartetTidspunkt
            )
            val p2AvsluttetPeriode = buildPeriode(
                id = p2Id,
                identitetsnummer = p2Identitetsnummer,
                startet = p2StartetTidspunkt,
                avsluttet = p2AvsluttetTidspunkt
            )

            val identitetsnummerSlot = slot<String>()
            every { kafkaKeysClientMock.hentKafkaKeys(capture(identitetsnummerSlot)) } answers {
                KafkaKeysResponse(
                    identitetsnummerSlot.captured.toLong(),
                    identitetsnummerSlot.captured.toLong()
                )
            }

            "Skal aktivere begge microfrontends ved start av periode eldre en 21 dager (p1)" {
                logger.info("Tester p1 startet {}", p1Id)
                periodeTopic.pipeInput(p1ArbeidssoekerId, p1StartetPeriode)

                microfrontendTopic.isEmpty shouldBe false
                val keyValueList = microfrontendTopic.readKeyValuesToList()
                keyValueList.size shouldBe 2

                val minSideKeyValue = keyValueList.first()
                val behovsvurderingKeyValue = keyValueList.last()

                minSideKeyValue.key shouldBe p1ArbeidssoekerId
                with(minSideKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.ENABLE
                    ident shouldBe p1StartetPeriode.identitetsnummer
                    microfrontendId shouldBe appConfig.microfrontends.aiaMinSide
                    sensitivitet shouldBe Sensitivitet.HIGH
                    initialedBy shouldBe "paw"
                }

                behovsvurderingKeyValue.key shouldBe p1ArbeidssoekerId
                with(behovsvurderingKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.ENABLE
                    ident shouldBe p1StartetPeriode.identitetsnummer
                    microfrontendId shouldBe appConfig.microfrontends.aiaBehovsvurdering
                    sensitivitet shouldBe Sensitivitet.HIGH
                    initialedBy shouldBe "paw"
                }

                periodeStateStore.size() shouldBe 1
                with(periodeStateStore.get(p1ArbeidssoekerId).shouldBeInstanceOf<PeriodeInfo>()) {
                    id shouldBe p1StartetPeriode.id
                    identitetsnummer shouldBe p1StartetPeriode.identitetsnummer
                    arbeidssoekerId shouldBe p1ArbeidssoekerId
                    startet shouldBe p1StartetPeriode.startet.tidspunkt
                    avsluttet shouldBe null
                }
            }

            "Skal deaktivere begge microfrontend ved avslutting av periode eldre enn 21 dager (p1)" {
                logger.info("Tester p1 avsluttet {}", p1Id)
                periodeTopic.pipeInput(p1ArbeidssoekerId, p1AvsluttetPeriode)

                microfrontendTopic.isEmpty shouldBe false
                val keyValueList = microfrontendTopic.readKeyValuesToList()
                keyValueList.size shouldBe 2

                val minSideKeyValue = keyValueList.first()
                val behovsvurderingKeyValue = keyValueList.last()

                minSideKeyValue.key shouldBe p1ArbeidssoekerId
                with(minSideKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.DISABLE
                    ident shouldBe p1StartetPeriode.identitetsnummer
                    microfrontendId shouldBe appConfig.microfrontends.aiaMinSide
                    sensitivitet shouldBe null
                    initialedBy shouldBe "paw"
                }

                behovsvurderingKeyValue.key shouldBe p1ArbeidssoekerId
                with(behovsvurderingKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.DISABLE
                    ident shouldBe p1AvsluttetPeriode.identitetsnummer
                    microfrontendId shouldBe appConfig.microfrontends.aiaBehovsvurdering
                    sensitivitet shouldBe null
                    initialedBy shouldBe "paw"
                }

                periodeStateStore.size() shouldBe 0
            }

            "Skal aktivere begge microfrontends ved start av periode (p2)" {
                logger.info("Tester p2 startet {}", p2Id)
                periodeTopic.pipeInput(p2ArbeidssoekerId, p2StartetPeriode)

                microfrontendTopic.isEmpty shouldBe false
                val keyValueList = microfrontendTopic.readKeyValuesToList()
                keyValueList.size shouldBe 2

                val minSideKeyValue = keyValueList.first()
                val behovsvurderingKeyValue = keyValueList.last()

                minSideKeyValue.key shouldBe p2Identitetsnummer.toLong()
                with(minSideKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.ENABLE
                    ident shouldBe p2StartetPeriode.identitetsnummer
                    microfrontendId shouldBe appConfig.microfrontends.aiaMinSide
                    sensitivitet shouldBe Sensitivitet.HIGH
                    initialedBy shouldBe "paw"
                }

                behovsvurderingKeyValue.key shouldBe p2ArbeidssoekerId
                with(behovsvurderingKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.ENABLE
                    ident shouldBe p2StartetPeriode.identitetsnummer
                    microfrontendId shouldBe appConfig.microfrontends.aiaBehovsvurdering
                    sensitivitet shouldBe Sensitivitet.HIGH
                    initialedBy shouldBe "paw"
                }

                periodeStateStore.size() shouldBe 1
                with(periodeStateStore.get(p2ArbeidssoekerId).shouldBeInstanceOf<PeriodeInfo>()) {
                    id shouldBe p2StartetPeriode.id
                    identitetsnummer shouldBe p2StartetPeriode.identitetsnummer
                    arbeidssoekerId shouldBe p2ArbeidssoekerId
                    startet shouldBe p2StartetPeriode.startet.tidspunkt
                    avsluttet shouldBe null
                }
            }

            "Skal deaktivere aia-behovsvurdering microfrontend ved avslutting av periode nyere enn 21 dager (p2)" {
                logger.info("Tester p2 avsluttet {}", p2Id)
                periodeTopic.pipeInput(p2ArbeidssoekerId, p2AvsluttetPeriode)

                microfrontendTopic.isEmpty shouldBe false
                val keyValueList = microfrontendTopic.readKeyValuesToList()
                keyValueList.size shouldBe 1

                val behovsvurderingKeyValue = keyValueList.last()

                behovsvurderingKeyValue.key shouldBe p2ArbeidssoekerId
                with(behovsvurderingKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.DISABLE
                    ident shouldBe p2AvsluttetPeriode.identitetsnummer
                    microfrontendId shouldBe appConfig.microfrontends.aiaBehovsvurdering
                    sensitivitet shouldBe null
                    initialedBy shouldBe "paw"
                }

                periodeStateStore.size() shouldBe 1
                with(periodeStateStore.get(p2ArbeidssoekerId).shouldBeInstanceOf<PeriodeInfo>()) {
                    id shouldBe p2AvsluttetPeriode.id
                    identitetsnummer shouldBe p2AvsluttetPeriode.identitetsnummer
                    arbeidssoekerId shouldBe p2ArbeidssoekerId
                    startet shouldBe p2AvsluttetPeriode.startet.tidspunkt
                    avsluttet shouldBe p2AvsluttetPeriode.avsluttet.tidspunkt
                }
            }

            "Skal deaktivere aia-min-side microfrontend 21 dager etter avslutting av periode (p2)" {
                logger.info("Tester p2 21 dager etter avluttet {}", p2Id)
                testDriver.advanceWallClockTime(Duration.ofDays(17)) // 17 dager før avsluttettidspunkt

                microfrontendTopic.isEmpty shouldBe false
                val keyValueList = microfrontendTopic.readKeyValuesToList()
                keyValueList.size shouldBe 1

                val behovsvurderingKeyValue = keyValueList.last()

                behovsvurderingKeyValue.key shouldBe p2ArbeidssoekerId
                with(behovsvurderingKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.DISABLE
                    ident shouldBe p2AvsluttetPeriode.identitetsnummer
                    microfrontendId shouldBe appConfig.microfrontends.aiaMinSide
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