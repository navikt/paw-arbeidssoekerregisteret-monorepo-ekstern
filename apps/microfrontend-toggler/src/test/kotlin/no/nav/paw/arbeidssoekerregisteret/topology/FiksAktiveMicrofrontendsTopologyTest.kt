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
import java.time.ZoneId
import java.util.*

/**
 *                 -21d                  -10d        -5d       Frist       +5d        +10d
 * <----------------|----------------------|----------|----------|----------|----------|--->
 *    |--- p1 -->|
 *                                         |--- p2 -->|
 *                                                          |--- p3 ----------------------->
 *                                                          |--- p4 -->|
 *                                                                          |--- p5 -->|
 *
 * p1: Skal lagres og føre til deaktivering av både aia-min-side og aia-behovsvurdering
 * p2: Skal lagres og føre til deaktivering av aia-behovsvurdering
 * p3: Skal lagres men ikke føre til deaktivering fordi perioden ikke ble avsluttet før frist
 * p4: Skal lagres men ikke føre til deaktivering fordi perioden ikke ble avsluttet før frist
 * p5: Skal filtreres ut fordi perioden ble både startet og avsluttet etter frist
 */
class FiksAktiveMicrofrontendsTopologyTest : FreeSpec({

    with(TestContext()) {
        "Testsuite for deaktivering av microfrontends basert på gamle avsluttede arbeidssøkerperioder" - {
            val deaktiveringsfrist = appConfig.regler.fiksAktiveMicrofrontendsForPerioderEldreEnn
                .atZone(ZoneId.systemDefault())
                .toInstant()
            val aiaMinSideAvsluttetfrist = deaktiveringsfrist.minus(appConfig.regler.utsattDeaktiveringAvAiaMinSide)

            val p1Id = UUID.randomUUID()
            val p1Identitetsnummer = "01017012345"
            val p1ArbeidssoekerId = p1Identitetsnummer.toLong()
            val p1StartetTidspunkt = aiaMinSideAvsluttetfrist.minus(Duration.ofDays(7))
            val p1AvsluttetTidspunkt = aiaMinSideAvsluttetfrist.minus(Duration.ofDays(2))
            val p1StartetPeriode = buildPeriode(
                id = p1Id,
                identitetsnummer = p1Identitetsnummer,
                startet = p1StartetTidspunkt,
            )
            val p1AvsluttetPeriode = buildPeriode(
                id = p1Id,
                identitetsnummer = p1Identitetsnummer,
                startet = p1StartetTidspunkt,
                avsluttet = p1AvsluttetTidspunkt
            )
            val p2Id = UUID.randomUUID()
            val p2Identitetsnummer = "02017012345"
            val p2ArbeidssoekerId = p2Identitetsnummer.toLong()
            val p2StartetTidspunkt = deaktiveringsfrist.minus(Duration.ofDays(10))
            val p2AvsluttetTidspunkt = deaktiveringsfrist.minus(Duration.ofDays(5))
            val p2StartetPeriode = buildPeriode(
                id = p2Id,
                identitetsnummer = p2Identitetsnummer,
                startet = p2StartetTidspunkt,
            )
            val p2AvsluttetPeriode = buildPeriode(
                id = p2Id,
                identitetsnummer = p2Identitetsnummer,
                startet = p2StartetTidspunkt,
                avsluttet = p2AvsluttetTidspunkt
            )
            val p3Id = UUID.randomUUID()
            val p3Identitetsnummer = "03017012345"
            val p3ArbeidsgiverId = p3Identitetsnummer.toLong()
            val p3StartetTidspunkt = deaktiveringsfrist.minus(Duration.ofDays(5))
            val p3StartetPeriode = buildPeriode(
                id = p3Id,
                identitetsnummer = p3Identitetsnummer,
                startet = p3StartetTidspunkt
            )
            val p4Id = UUID.randomUUID()
            val p4Identitetsnummer = "04017012345"
            val p4ArbeidsgiverId = p4Identitetsnummer.toLong()
            val p4StartetTidspunkt = deaktiveringsfrist.minus(Duration.ofDays(5))
            val p4AvsluttetTidspunkt = deaktiveringsfrist.plus(Duration.ofDays(5))
            val p4StartetPeriode = buildPeriode(
                id = p4Id,
                identitetsnummer = p4Identitetsnummer,
                startet = p4StartetTidspunkt
            )
            val p4AvsluttetPeriode = buildPeriode(
                id = p4Id,
                identitetsnummer = p4Identitetsnummer,
                startet = p4StartetTidspunkt,
                avsluttet = p4AvsluttetTidspunkt
            )
            val p5Id = UUID.randomUUID()
            val p5Identitetsnummer = "05017012345"
            val p5ArbeidssoekerId = p5Identitetsnummer.toLong()
            val p5StartetTidspunkt = deaktiveringsfrist.plus(Duration.ofDays(5))
            val p5AvsluttetTidspunkt = deaktiveringsfrist.plus(Duration.ofDays(10))
            val p5StartetPeriode = buildPeriode(
                id = p5Id,
                identitetsnummer = p5Identitetsnummer,
                startet = p5StartetTidspunkt
            )
            val p5AvsluttetPeriode = buildPeriode(
                id = p5Id,
                identitetsnummer = p5Identitetsnummer,
                startet = p5StartetTidspunkt,
                avsluttet = p5AvsluttetTidspunkt
            )

            val identitetsnummerSlot = slot<String>()
            every { kafkaKeysClientMock.hentKafkaKeys(capture(identitetsnummerSlot)) } answers {
                KafkaKeysResponse(
                    identitetsnummerSlot.captured.toLong(),
                    identitetsnummerSlot.captured.toLong()
                )
            }

            "Skal lagre avsluttet periode eldre enn 21 dager før frist (p1)" {
                logger.info("Tester p1 {}", p1Id)
                periodeTopic.pipeInput(p1ArbeidssoekerId, p1StartetPeriode, p1StartetTidspunkt)
                periodeTopic.pipeInput(p1ArbeidssoekerId, p1AvsluttetPeriode, p1AvsluttetTidspunkt)

                microfrontendTopic.isEmpty shouldBe true
                internalStateStore.size() shouldBe 1
            }

            "Skal lagre avsluttet periode eldre enn frist (p2)" {
                logger.info("Tester p2 {}", p2Id)
                periodeTopic.pipeInput(p2ArbeidssoekerId, p2StartetPeriode, p2StartetTidspunkt)
                periodeTopic.pipeInput(p2ArbeidssoekerId, p2AvsluttetPeriode, p2AvsluttetTidspunkt)

                microfrontendTopic.isEmpty shouldBe true
                internalStateStore.size() shouldBe 2
            }

            "Skal lagre aktiv periode (p3)" {
                logger.info("Tester p3 {}", p3Id)
                periodeTopic.pipeInput(p3ArbeidsgiverId, p3StartetPeriode, p3StartetTidspunkt)

                microfrontendTopic.isEmpty shouldBe true
                internalStateStore.size() shouldBe 3
            }

            "Skal lagre periode avsluttet etter frist (p4)" {
                logger.info("Tester p4 {}", p4Id)
                periodeTopic.pipeInput(p4ArbeidsgiverId, p4StartetPeriode, p4StartetTidspunkt)
                periodeTopic.pipeInput(p4ArbeidsgiverId, p4AvsluttetPeriode, p4AvsluttetTidspunkt)

                microfrontendTopic.isEmpty shouldBe true
                internalStateStore.size() shouldBe 4
            }

            "Skal filtrere ut periode startet og avluttet etter frist (p5)" {
                logger.info("Tester p5 {}", p5Id)
                periodeTopic.pipeInput(p5ArbeidssoekerId, p5StartetPeriode, p5StartetTidspunkt)
                periodeTopic.pipeInput(p5ArbeidssoekerId, p5AvsluttetPeriode, p5AvsluttetTidspunkt)

                microfrontendTopic.isEmpty shouldBe true
                internalStateStore.size() shouldBe 4
            }

            "Skal ikke gjøre noe etter 10t fordi punctuator ikke har kjørt ennå" {
                microfrontendTopic.isEmpty shouldBe true
                internalStateStore.size() shouldBe 4

                testDriver.advanceWallClockTime(Duration.ofHours(10))

                microfrontendTopic.isEmpty shouldBe true
                internalStateStore.size() shouldBe 4
            }

            "Skal deaktivere microfrontends for eldre perioder (p1 og p2) etter 13t når punctuator har kjørt" {
                microfrontendTopic.isEmpty shouldBe true
                internalStateStore.size() shouldBe 4

                testDriver.advanceWallClockTime(Duration.ofHours(3))

                microfrontendTopic.isEmpty shouldBe false
                val keyValueList = microfrontendTopic.readKeyValuesToList()
                keyValueList.size shouldBe 3

                val p1MinSideKeyValue = keyValueList[0]
                val p1BehovsvurderingKeyValue = keyValueList[1]
                val p2BehovsvurderingKeyValue = keyValueList[2]

                p1MinSideKeyValue.key shouldBe p1ArbeidssoekerId
                with(p1MinSideKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.DISABLE
                    ident shouldBe p1Identitetsnummer
                    microfrontendId shouldBe appConfig.microfrontends.aiaMinSide
                    sensitivitet shouldBe null
                    initialedBy shouldBe "paw"
                }

                p1BehovsvurderingKeyValue.key shouldBe p1ArbeidssoekerId
                with(p1BehovsvurderingKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.DISABLE
                    ident shouldBe p1Identitetsnummer
                    microfrontendId shouldBe appConfig.microfrontends.aiaBehovsvurdering
                    sensitivitet shouldBe null
                    initialedBy shouldBe "paw"
                }

                p2BehovsvurderingKeyValue.key shouldBe p2ArbeidssoekerId
                with(p2BehovsvurderingKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.DISABLE
                    ident shouldBe p2Identitetsnummer
                    microfrontendId shouldBe appConfig.microfrontends.aiaBehovsvurdering
                    sensitivitet shouldBe null
                    initialedBy shouldBe "paw"
                }

                internalStateStore.size() shouldBe 0
            }

            afterSpec {
                testDriver.close()
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
        val toggleSerde = buildToggleSerde()
        val kafkaKeysClientMock = mockk<KafkaKeysClientMock>()

        val testDriver =
            with(ConfigContext(appConfig)) {
                with(LoggingContext(logger, auditLogger)) {
                    StreamsBuilder().apply {
                        addStateStore(
                            Stores.keyValueStoreBuilder(
                                Stores.inMemoryKeyValueStore(FIKS_AKTIVE_MICROFRONTENDS_TOGGLE_STATE_STORE),
                                Serdes.Long(),
                                buildPeriodeInfoSerde()
                            )
                        )
                        addFiksAktiveMicrofrontendsStream(
                            meterRegistry,
                            kafkaKeysClientMock::hentKafkaKeys
                        )
                    }.build()
                }
            }.let { TopologyTestDriver(it, kafkaStreamProperties) }

        val internalStateStore =
            testDriver.getKeyValueStore<Long, PeriodeInfo>(FIKS_AKTIVE_MICROFRONTENDS_TOGGLE_STATE_STORE)

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