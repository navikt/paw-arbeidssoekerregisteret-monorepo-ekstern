package no.nav.paw.arbeidssoekerregisteret.topology

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearAllMocks
import io.mockk.coEvery
import no.nav.paw.arbeidssoekerregisteret.model.PeriodeInfo
import no.nav.paw.arbeidssoekerregisteret.model.Sensitivitet
import no.nav.paw.arbeidssoekerregisteret.model.Toggle
import no.nav.paw.arbeidssoekerregisteret.model.ToggleAction
import no.nav.paw.arbeidssoekerregisteret.test.TestContext
import no.nav.paw.arbeidssoekerregisteret.test.TestData
import no.nav.paw.arbeidssoekerregisteret.test.shouldBeEqualTo
import no.nav.paw.arbeidssoekerregisteret.topology.streams.buildPeriodeStream
import no.nav.paw.arbeidssoekerregisteret.utils.getIdAndKeyBlocking
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.state.Stores
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
class PeriodeStreamTest : FreeSpec({

    with(LocalTestContext()) {
        with(LocalTestData()) {

            beforeSpec {
                clearAllMocks()
            }

            "Testsuite for toggling av microfrontends basert på arbeidssøkerperiode" - {

                "Skal aktivere begge microfrontends ved start av periode eldre en 21 dager (p1)" {
                    coEvery { kafkaKeysClientMock.getIdAndKey(p1Identitetsnummer.verdi) } returns KafkaKeysResponse(
                        p1ArbeidssoekerId,
                        1
                    )

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
                        microfrontendId shouldBe applicationConfig.microfrontendToggle.aiaMinSide
                        sensitivitet shouldBe Sensitivitet.SUBSTANTIAL
                        initialedBy shouldBe "paw"
                    }

                    behovsvurderingKeyValue.key shouldBe p1ArbeidssoekerId
                    with(behovsvurderingKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                        action shouldBe ToggleAction.ENABLE
                        ident shouldBe p1StartetPeriode.identitetsnummer
                        microfrontendId shouldBe applicationConfig.microfrontendToggle.aiaBehovsvurdering
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
                    coEvery { kafkaKeysClientMock.getIdAndKey(p2Identitetsnummer.verdi) } returns KafkaKeysResponse(
                        p1ArbeidssoekerId,
                        1
                    )

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
                        microfrontendId shouldBe applicationConfig.microfrontendToggle.aiaMinSide
                        sensitivitet shouldBe null
                        initialedBy shouldBe "paw"
                    }

                    behovsvurderingKeyValue.key shouldBe p1ArbeidssoekerId
                    with(behovsvurderingKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                        action shouldBe ToggleAction.DISABLE
                        ident shouldBe p1AvsluttetPeriode.identitetsnummer
                        microfrontendId shouldBe applicationConfig.microfrontendToggle.aiaBehovsvurdering
                        sensitivitet shouldBe null
                        initialedBy shouldBe "paw"
                    }

                    periodeStateStore.size() shouldBe 0
                }

                "Skal aktivere begge microfrontends ved start av periode (p2)" {
                    coEvery { kafkaKeysClientMock.getIdAndKey(p2Identitetsnummer.verdi) } returns KafkaKeysResponse(
                        p2ArbeidssoekerId,
                        1
                    )

                    periodeTopic.pipeInput(p2ArbeidssoekerId, p2StartetPeriode)

                    microfrontendTopic.isEmpty shouldBe false
                    val keyValueList = microfrontendTopic.readKeyValuesToList()
                    keyValueList.size shouldBe 2

                    val minSideKeyValue = keyValueList.first()
                    val behovsvurderingKeyValue = keyValueList.last()

                    minSideKeyValue.key shouldBe p2ArbeidssoekerId
                    with(minSideKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                        action shouldBe ToggleAction.ENABLE
                        ident shouldBe p2StartetPeriode.identitetsnummer
                        microfrontendId shouldBe applicationConfig.microfrontendToggle.aiaMinSide
                        sensitivitet shouldBe Sensitivitet.SUBSTANTIAL
                        initialedBy shouldBe "paw"
                    }

                    behovsvurderingKeyValue.key shouldBe p2ArbeidssoekerId
                    with(behovsvurderingKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                        action shouldBe ToggleAction.ENABLE
                        ident shouldBe p2StartetPeriode.identitetsnummer
                        microfrontendId shouldBe applicationConfig.microfrontendToggle.aiaBehovsvurdering
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
                    coEvery { kafkaKeysClientMock.getIdAndKey(p2Identitetsnummer.verdi) } returns KafkaKeysResponse(
                        p2ArbeidssoekerId,
                        1
                    )

                    periodeTopic.pipeInput(p2ArbeidssoekerId, p2AvsluttetPeriode)

                    microfrontendTopic.isEmpty shouldBe false
                    val keyValueList = microfrontendTopic.readKeyValuesToList()
                    keyValueList.size shouldBe 1

                    val behovsvurderingKeyValue = keyValueList.last()

                    behovsvurderingKeyValue.key shouldBe p2ArbeidssoekerId
                    with(behovsvurderingKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                        action shouldBe ToggleAction.DISABLE
                        ident shouldBe p2AvsluttetPeriode.identitetsnummer
                        microfrontendId shouldBe applicationConfig.microfrontendToggle.aiaBehovsvurdering
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
                    coEvery { kafkaKeysClientMock.getIdAndKey(p2Identitetsnummer.verdi) } returns KafkaKeysResponse(
                        p2ArbeidssoekerId,
                        1
                    )

                    testDriver.advanceWallClockTime(Duration.ofDays(17)) // 17 dager før avsluttettidspunkt

                    microfrontendTopic.isEmpty shouldBe false
                    val keyValueList = microfrontendTopic.readKeyValuesToList()
                    keyValueList.size shouldBe 1

                    val behovsvurderingKeyValue = keyValueList.last()

                    behovsvurderingKeyValue.key shouldBe p2ArbeidssoekerId
                    with(behovsvurderingKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                        action shouldBe ToggleAction.DISABLE
                        ident shouldBe p2AvsluttetPeriode.identitetsnummer
                        microfrontendId shouldBe applicationConfig.microfrontendToggle.aiaMinSide
                        sensitivitet shouldBe null
                        initialedBy shouldBe "paw"
                    }

                    periodeStateStore.size() shouldBe 0
                }

                "Skal ignorere duplikat start periode" {
                    coEvery { kafkaKeysClientMock.getIdAndKey(p3Identitetsnummer.verdi) } returns KafkaKeysResponse(
                        p3ArbeidssoekerId,
                        1
                    )

                    microfrontendTopic.isEmpty shouldBe true
                    periodeTopic.pipeInput(p3ArbeidssoekerId, p3StartetPeriode1)

                    periodeStateStore.size() shouldBe 1
                    val periodeInfo1 = periodeStateStore.get(p3ArbeidssoekerId)
                    periodeInfo1 shouldNotBe null
                    periodeInfo1.arbeidssoekerId shouldBe p3ArbeidssoekerId
                    periodeInfo1 shouldBeEqualTo p3StartetPeriode1

                    periodeTopic.pipeInput(p3ArbeidssoekerId, p3StartetPeriode2)

                    periodeStateStore.size() shouldBe 1
                    val periodeInfo2 = periodeStateStore.get(p3ArbeidssoekerId)
                    periodeInfo2 shouldNotBe null
                    periodeInfo2.arbeidssoekerId shouldBe p3ArbeidssoekerId
                    periodeInfo2 shouldBeEqualTo p3StartetPeriode1
                }
            }
        }
    }
}) {
    private class LocalTestContext : TestContext() {

        val testDriver = StreamsBuilder().apply {
            addStateStore(
                Stores.keyValueStoreBuilder(
                    Stores.inMemoryKeyValueStore(applicationConfig.kafkaTopology.periodeStoreName),
                    Serdes.Long(),
                    periodeInfoSerde
                )
            )
            buildPeriodeStream(
                applicationConfig,
                meterRegistry,
                kafkaKeysClientMock::getIdAndKeyBlocking
            )
        }.build()
            .let { TopologyTestDriver(it, kafkaStreamProperties) }

        val periodeStateStore =
            testDriver.getKeyValueStore<Long, PeriodeInfo>(applicationConfig.kafkaTopology.periodeStoreName)

        val periodeTopic = testDriver.createInputTopic(
            applicationConfig.kafkaTopology.periodeTopic,
            Serdes.Long().serializer(),
            periodeSerde.serializer()
        )

        val microfrontendTopic = testDriver.createOutputTopic(
            applicationConfig.kafkaTopology.microfrontendTopic,
            Serdes.Long().deserializer(),
            toggleSerde.deserializer()
        )
    }

    private class LocalTestData {
        val p1Id = TestData.periodeId1
        val p1Identitetsnummer = TestData.identitetsnummer2
        val p1ArbeidssoekerId = TestData.arbeidsoekerId2
        val p1AvsluttetTidspunkt = Instant.now().minus(Duration.ofDays(22))
        val p1StartetTidspunkt = p1AvsluttetTidspunkt.minus(Duration.ofDays(5))
        val p1StartetPeriode = TestData.buildPeriode(
            id = p1Id,
            identitetsnummer = p1Identitetsnummer,
            startetTidspunkt = p1StartetTidspunkt
        )
        val p1AvsluttetPeriode = TestData.buildPeriode(
            id = p1Id,
            identitetsnummer = p1Identitetsnummer,
            startetTidspunkt = p1StartetTidspunkt,
            avsluttetTidspunkt = p1AvsluttetTidspunkt
        )

        val p2Id = UUID.randomUUID()
        val p2Identitetsnummer = TestData.identitetsnummer1
        val p2ArbeidssoekerId = TestData.arbeidsoekerId1
        val p2AvsluttetTidspunkt = Instant.now().minus(Duration.ofDays(5))
        val p2StartetTidspunkt = p2AvsluttetTidspunkt.minus(Duration.ofDays(10))
        val p2StartetPeriode = TestData.buildPeriode(
            id = p2Id,
            identitetsnummer = p2Identitetsnummer,
            startetTidspunkt = p2StartetTidspunkt
        )
        val p2AvsluttetPeriode = TestData.buildPeriode(
            id = p2Id,
            identitetsnummer = p2Identitetsnummer,
            startetTidspunkt = p2StartetTidspunkt,
            avsluttetTidspunkt = p2AvsluttetTidspunkt
        )

        val p3Id = UUID.randomUUID()
        val p3Identitetsnummer = TestData.identitetsnummer3
        val p3ArbeidssoekerId = TestData.arbeidsoekerId3
        val p3StartetTidspunkt1 = Instant.now().minus(Duration.ofDays(30))
        val p3StartetTidspunkt2 = Instant.now().minus(Duration.ofDays(60))
        val p3StartetPeriode1 = TestData.buildPeriode(
            id = p3Id,
            identitetsnummer = p3Identitetsnummer,
            startetTidspunkt = p3StartetTidspunkt1
        )
        val p3StartetPeriode2 = TestData.buildPeriode(
            id = p3Id,
            identitetsnummer = p3Identitetsnummer,
            startetTidspunkt = p3StartetTidspunkt2
        )
    }
}