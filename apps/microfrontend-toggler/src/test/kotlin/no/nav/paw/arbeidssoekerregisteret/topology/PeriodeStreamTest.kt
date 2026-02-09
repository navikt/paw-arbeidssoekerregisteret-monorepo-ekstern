package no.nav.paw.arbeidssoekerregisteret.topology

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearAllMocks
import io.mockk.coEvery
import no.nav.paw.arbeidssoekerregisteret.config.APPLICATION_CONFIG
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.model.MicroFrontend
import no.nav.paw.arbeidssoekerregisteret.model.PeriodeInfo
import no.nav.paw.arbeidssoekerregisteret.model.Sensitivitet
import no.nav.paw.arbeidssoekerregisteret.model.Toggle
import no.nav.paw.arbeidssoekerregisteret.model.ToggleAction
import no.nav.paw.arbeidssoekerregisteret.model.asPeriodeInfo
import no.nav.paw.arbeidssoekerregisteret.test.TestContext
import no.nav.paw.arbeidssoekerregisteret.test.TestData
import no.nav.paw.arbeidssoekerregisteret.test.addPeriodeInMemoryStateStore
import no.nav.paw.arbeidssoekerregisteret.topology.streams.addPeriodeStream
import no.nav.paw.arbeidssoekerregisteret.utils.getIdAndKeyBlocking
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.kafkakeygenerator.model.KafkaKeysResponse
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.TestInputTopic
import org.apache.kafka.streams.TestOutputTopic
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.state.KeyValueStore
import java.time.Duration

/**
 *                 -21d                  -10d        -5d         Nå
 * <----------------|----------------------|----------|----------|----->
 *    |--- p1 -->|
 *                                         |--- p2 -->|
 *
 * p1: Skal aktivere aia-min-side ved start og deaktiver ved avslutting
 * p2: Skal aktivere aia-min-side ved start. Når det er gått > 21d siden avslutting skal aia-min-side deaktiveres.
 *
 */
class PeriodeStreamTest : FreeSpec({

    beforeSpec {
        clearAllMocks()
    }

    "Testsuite for toggling av microfrontends basert på arbeidssøkerperiode" - {
        val applicationConfig: ApplicationConfig = loadNaisOrLocalConfiguration(APPLICATION_CONFIG)
        with(LocalTestContext(applicationConfig = applicationConfig)) {

            "Skal aktivere aia-min-side ved start av periode eldre en 21 dager (p1)" {
                val kafkaKey = TestData.kafkaKey1
                val identitetsnummer = TestData.identitetsnummer1
                val arbeidssoekerId = TestData.arbeidsoekerId1
                val startetPeriode = TestData.periode1Startet

                coEvery { kafkaKeysClientMock.getIdAndKey(identitetsnummer.value) } returns KafkaKeysResponse(
                    arbeidssoekerId.value,
                    kafkaKey.value
                )

                periodeTopic.pipeInput(arbeidssoekerId.value, startetPeriode)

                microfrontendTopic.isEmpty shouldBe false
                val keyValueList = microfrontendTopic.readKeyValuesToList()
                keyValueList.size shouldBe 1

                val minSideKeyValue = keyValueList.first()

                minSideKeyValue.key shouldBe arbeidssoekerId.value
                with(minSideKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.ENABLE
                    ident shouldBe startetPeriode.identitetsnummer
                    microfrontendId shouldBe MicroFrontend.AIA_MIN_SIDE
                    sensitivitet shouldBe Sensitivitet.SUBSTANTIAL
                    initiatedBy shouldBe "paw"
                }

                periodeStateStore.size() shouldBe 1
                val periodeInfo = periodeStateStore.get(arbeidssoekerId.value)
                periodeInfo.shouldBeInstanceOf<PeriodeInfo> {
                    it.id shouldBe startetPeriode.id
                    it.identitetsnummer shouldBe startetPeriode.identitetsnummer
                    it.arbeidssoekerId shouldBe arbeidssoekerId.value
                    it.startet shouldBe startetPeriode.startet.tidspunkt
                    it.avsluttet shouldBe null
                }
            }

            "Skal deaktivere aia-min-side ved avslutting av periode eldre enn 21 dager (p1)" {
                val kafkaKey = TestData.kafkaKey1
                val identitetsnummer = TestData.identitetsnummer1
                val arbeidssoekerId = TestData.arbeidsoekerId1
                val avsluttetPeriode = TestData.periode1Avsluttet

                coEvery { kafkaKeysClientMock.getIdAndKey(identitetsnummer.value) } returns KafkaKeysResponse(
                    arbeidssoekerId.value,
                    kafkaKey.value
                )

                periodeTopic.pipeInput(arbeidssoekerId.value, avsluttetPeriode)

                microfrontendTopic.isEmpty shouldBe false
                val keyValueList = microfrontendTopic.readKeyValuesToList()
                keyValueList.size shouldBe 1

                val minSideKeyValue = keyValueList.first()

                minSideKeyValue.key shouldBe arbeidssoekerId.value
                with(minSideKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.DISABLE
                    ident shouldBe identitetsnummer.value
                    microfrontendId shouldBe MicroFrontend.AIA_MIN_SIDE
                    sensitivitet shouldBe null
                    initiatedBy shouldBe "paw"
                }

                periodeStateStore.size() shouldBe 0
            }

            "Skal aktivere aia-min-side ved start av periode (p2)" {
                val kafkaKey = TestData.kafkaKey2
                val identitetsnummer = TestData.identitetsnummer2
                val arbeidssoekerId = TestData.arbeidsoekerId2
                val startetPeriode = TestData.periode2Startet

                coEvery { kafkaKeysClientMock.getIdAndKey(identitetsnummer.value) } returns KafkaKeysResponse(
                    arbeidssoekerId.value,
                    kafkaKey.value
                )

                periodeTopic.pipeInput(arbeidssoekerId.value, startetPeriode)

                microfrontendTopic.isEmpty shouldBe false
                val keyValueList = microfrontendTopic.readKeyValuesToList()
                keyValueList.size shouldBe 1

                val minSideKeyValue = keyValueList.first()

                minSideKeyValue.key shouldBe arbeidssoekerId.value
                with(minSideKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.ENABLE
                    ident shouldBe identitetsnummer.value
                    microfrontendId shouldBe MicroFrontend.AIA_MIN_SIDE
                    sensitivitet shouldBe Sensitivitet.SUBSTANTIAL
                    initiatedBy shouldBe "paw"
                }

                periodeStateStore.size() shouldBe 1
                val periodeInfo = periodeStateStore.get(arbeidssoekerId.value)
                periodeInfo.shouldBeInstanceOf<PeriodeInfo> {
                    it.id shouldBe startetPeriode.id
                    it.identitetsnummer shouldBe identitetsnummer.value
                    it.arbeidssoekerId shouldBe arbeidssoekerId.value
                    it.startet shouldBe startetPeriode.startet.tidspunkt
                    it.avsluttet shouldBe null
                }
            }

            "Skal ikke deaktivere aia-min-side ved avslutting av periode nyere enn 21 dager (p2)" {
                val kafkaKey = TestData.kafkaKey2
                val identitetsnummer = TestData.identitetsnummer2
                val arbeidssoekerId = TestData.arbeidsoekerId2
                val avsluttetPeriode = TestData.periode2Avsluttet

                coEvery { kafkaKeysClientMock.getIdAndKey(identitetsnummer.value) } returns KafkaKeysResponse(
                    arbeidssoekerId.value,
                    kafkaKey.value
                )

                periodeTopic.pipeInput(arbeidssoekerId.value, avsluttetPeriode)

                microfrontendTopic.isEmpty shouldBe true
                val keyValueList = microfrontendTopic.readKeyValuesToList()
                keyValueList.size shouldBe 0

                periodeStateStore.size() shouldBe 1
                val periodeInfo = periodeStateStore.get(arbeidssoekerId.value)
                periodeInfo.shouldBeInstanceOf<PeriodeInfo> {
                    it.id shouldBe avsluttetPeriode.id
                    it.identitetsnummer shouldBe identitetsnummer.value
                    it.arbeidssoekerId shouldBe arbeidssoekerId.value
                    it.startet shouldBe avsluttetPeriode.startet.tidspunkt
                    it.avsluttet shouldBe avsluttetPeriode.avsluttet.tidspunkt
                }
            }

            "Skal deaktivere aia-min-side microfrontend 21 dager etter avslutting av periode (p2)" {
                val kafkaKey = TestData.kafkaKey2
                val identitetsnummer = TestData.identitetsnummer2
                val arbeidssoekerId = TestData.arbeidsoekerId2

                coEvery { kafkaKeysClientMock.getIdAndKey(identitetsnummer.value) } returns KafkaKeysResponse(
                    arbeidssoekerId.value,
                    kafkaKey.value
                )

                testDriver.advanceWallClockTime(Duration.ofDays(17)) // 17 dager før avsluttettidspunkt

                microfrontendTopic.isEmpty shouldBe false
                val keyValueList = microfrontendTopic.readKeyValuesToList()
                keyValueList.size shouldBe 1

                val behovsvurderingKeyValue = keyValueList.last()

                behovsvurderingKeyValue.key shouldBe arbeidssoekerId.value
                with(behovsvurderingKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.DISABLE
                    ident shouldBe identitetsnummer.value
                    microfrontendId shouldBe MicroFrontend.AIA_MIN_SIDE
                    sensitivitet shouldBe null
                    initiatedBy shouldBe "paw"
                }

                periodeStateStore.size() shouldBe 0
            }

            "Skal ignorere duplikat start periode" {
                val kafkaKey = TestData.kafkaKey3
                val identitetsnummer = TestData.identitetsnummer3
                val arbeidssoekerId = TestData.arbeidsoekerId3
                val startetPeriode1 = TestData.periode3Startet1
                val startetPeriode2 = TestData.periode3Startet2

                coEvery { kafkaKeysClientMock.getIdAndKey(identitetsnummer.value) } returns KafkaKeysResponse(
                    arbeidssoekerId.value,
                    kafkaKey.value
                )

                microfrontendTopic.isEmpty shouldBe true
                periodeTopic.pipeInput(arbeidssoekerId.value, startetPeriode1)

                periodeStateStore.size() shouldBe 1
                val periodeInfo1 = periodeStateStore.get(arbeidssoekerId.value)
                periodeInfo1 shouldNotBe null
                periodeInfo1.arbeidssoekerId shouldBe arbeidssoekerId.value
                periodeInfo1 shouldBe startetPeriode1.asPeriodeInfo(arbeidssoekerId.value)

                periodeTopic.pipeInput(arbeidssoekerId.value, startetPeriode2)

                periodeStateStore.size() shouldBe 1
                val periodeInfo2 = periodeStateStore.get(arbeidssoekerId.value)
                periodeInfo2 shouldNotBe null
                periodeInfo2.arbeidssoekerId shouldBe arbeidssoekerId.value
                periodeInfo2 shouldBe startetPeriode1.asPeriodeInfo(arbeidssoekerId.value)
            }
        }
    }
})

private class LocalTestContext(
    override val applicationConfig: ApplicationConfig
) : TestContext(applicationConfig = applicationConfig) {

    val testDriver = StreamsBuilder().apply {
        addPeriodeInMemoryStateStore(applicationConfig)
        addPeriodeStream(
            applicationConfig,
            meterRegistry,
            kafkaKeysClientMock::getIdAndKeyBlocking
        )
    }.build()
        .let { TopologyTestDriver(it, kafkaStreamProperties) }

    val periodeStateStore: KeyValueStore<Long, PeriodeInfo> = testDriver
        .getKeyValueStore(applicationConfig.kafkaTopology.periodeStateStore)

    val periodeTopic: TestInputTopic<Long, Periode> = testDriver.createInputTopic(
        applicationConfig.kafkaTopology.periodeTopic,
        Serdes.Long().serializer(),
        periodeSerde.serializer()
    )

    val microfrontendTopic: TestOutputTopic<Long, Toggle> = testDriver.createOutputTopic(
        applicationConfig.kafkaTopology.microfrontendTopic,
        Serdes.Long().deserializer(),
        toggleSerde.deserializer()
    )
}