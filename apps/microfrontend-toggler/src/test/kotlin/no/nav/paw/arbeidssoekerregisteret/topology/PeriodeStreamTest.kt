package no.nav.paw.arbeidssoekerregisteret.topology

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearAllMocks
import io.mockk.coEvery
import no.nav.paw.arbeidssoekerregisteret.config.APPLICATION_CONFIG
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.model.PeriodeInfo
import no.nav.paw.arbeidssoekerregisteret.model.Sensitivitet
import no.nav.paw.arbeidssoekerregisteret.model.Toggle
import no.nav.paw.arbeidssoekerregisteret.model.ToggleAction
import no.nav.paw.arbeidssoekerregisteret.model.asPeriodeInfo
import no.nav.paw.arbeidssoekerregisteret.test.TestContext
import no.nav.paw.arbeidssoekerregisteret.test.TestData
import no.nav.paw.arbeidssoekerregisteret.test.TestData.buildPeriode
import no.nav.paw.arbeidssoekerregisteret.topology.streams.addPeriodeStream
import no.nav.paw.arbeidssoekerregisteret.utils.getIdAndKeyBlocking
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.kafkakeygenerator.model.KafkaKeysResponse
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

    beforeSpec {
        clearAllMocks()
    }

    "Testsuite for toggling av microfrontends basert på arbeidssøkerperiode" - {
        val applicationConfig: ApplicationConfig = loadNaisOrLocalConfiguration(APPLICATION_CONFIG)
        with(LocalTestContext(applicationConfig = applicationConfig)) {

            "Skal aktivere begge microfrontends ved start av periode eldre en 21 dager (p1)" {
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
                keyValueList.size shouldBe 2

                val minSideKeyValue = keyValueList.first()
                val behovsvurderingKeyValue = keyValueList.last()

                minSideKeyValue.key shouldBe arbeidssoekerId.value
                with(minSideKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.ENABLE
                    ident shouldBe startetPeriode.identitetsnummer
                    microfrontendId shouldBe applicationConfig.microfrontendToggle.aiaMinSide
                    sensitivitet shouldBe Sensitivitet.SUBSTANTIAL
                    initiatedBy shouldBe "paw"
                }

                behovsvurderingKeyValue.key shouldBe arbeidssoekerId.value
                with(behovsvurderingKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.ENABLE
                    ident shouldBe startetPeriode.identitetsnummer
                    microfrontendId shouldBe applicationConfig.microfrontendToggle.aiaBehovsvurdering
                    sensitivitet shouldBe Sensitivitet.HIGH
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

            "Skal deaktivere begge microfrontend ved avslutting av periode eldre enn 21 dager (p1)" {
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
                keyValueList.size shouldBe 2

                val minSideKeyValue = keyValueList.first()
                val behovsvurderingKeyValue = keyValueList.last()

                minSideKeyValue.key shouldBe arbeidssoekerId.value
                with(minSideKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.DISABLE
                    ident shouldBe identitetsnummer.value
                    microfrontendId shouldBe applicationConfig.microfrontendToggle.aiaMinSide
                    sensitivitet shouldBe null
                    initiatedBy shouldBe "paw"
                }

                behovsvurderingKeyValue.key shouldBe arbeidssoekerId.value
                with(behovsvurderingKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.DISABLE
                    ident shouldBe identitetsnummer.value
                    microfrontendId shouldBe applicationConfig.microfrontendToggle.aiaBehovsvurdering
                    sensitivitet shouldBe null
                    initiatedBy shouldBe "paw"
                }

                periodeStateStore.size() shouldBe 0
            }

            "Skal aktivere begge microfrontends ved start av periode (p2)" {
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
                keyValueList.size shouldBe 2

                val minSideKeyValue = keyValueList.first()
                val behovsvurderingKeyValue = keyValueList.last()

                minSideKeyValue.key shouldBe arbeidssoekerId.value
                with(minSideKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.ENABLE
                    ident shouldBe identitetsnummer.value
                    microfrontendId shouldBe applicationConfig.microfrontendToggle.aiaMinSide
                    sensitivitet shouldBe Sensitivitet.SUBSTANTIAL
                    initiatedBy shouldBe "paw"
                }

                behovsvurderingKeyValue.key shouldBe arbeidssoekerId.value
                with(behovsvurderingKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.ENABLE
                    ident shouldBe identitetsnummer.value
                    microfrontendId shouldBe applicationConfig.microfrontendToggle.aiaBehovsvurdering
                    sensitivitet shouldBe Sensitivitet.HIGH
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

            "Skal deaktivere aia-behovsvurdering microfrontend ved avslutting av periode nyere enn 21 dager (p2)" {
                val kafkaKey = TestData.kafkaKey2
                val identitetsnummer = TestData.identitetsnummer2
                val arbeidssoekerId = TestData.arbeidsoekerId2
                val avsluttetPeriode = TestData.periode2Avsluttet

                coEvery { kafkaKeysClientMock.getIdAndKey(identitetsnummer.value) } returns KafkaKeysResponse(
                    arbeidssoekerId.value,
                    kafkaKey.value
                )

                periodeTopic.pipeInput(arbeidssoekerId.value, avsluttetPeriode)

                microfrontendTopic.isEmpty shouldBe false
                val keyValueList = microfrontendTopic.readKeyValuesToList()
                keyValueList.size shouldBe 1

                val behovsvurderingKeyValue = keyValueList.last()

                behovsvurderingKeyValue.key shouldBe arbeidssoekerId.value
                with(behovsvurderingKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.DISABLE
                    ident shouldBe identitetsnummer.value
                    microfrontendId shouldBe applicationConfig.microfrontendToggle.aiaBehovsvurdering
                    sensitivitet shouldBe null
                    initiatedBy shouldBe "paw"
                }

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
                    microfrontendId shouldBe applicationConfig.microfrontendToggle.aiaMinSide
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

    "Testsuite for deprekering av behovsvurdering" - {
        val deprekeringstidspunkt = Instant.now()
        val applicationConfig: ApplicationConfig = loadNaisOrLocalConfiguration(APPLICATION_CONFIG)
        val justertApplicationConfig = applicationConfig.copy(
            deprekering = applicationConfig.deprekering.copy(
                tidspunkt = deprekeringstidspunkt
            )
        )

        with(LocalTestContext(applicationConfig = justertApplicationConfig)) {

            "Skal aktivere behovsvurdering om periode er startet før deprekeringstidspunkt $deprekeringstidspunkt" {
                val kafkaKey = TestData.kafkaKey5
                val identitetsnummer = TestData.identitetsnummer5
                val arbeidssoekerId = TestData.arbeidsoekerId5
                val startetPeriode = buildPeriode(
                    id = UUID.randomUUID(),
                    identitetsnummer = identitetsnummer,
                    startetTidspunkt = deprekeringstidspunkt.minusSeconds(60)
                )

                coEvery { kafkaKeysClientMock.getIdAndKey(identitetsnummer.value) } returns KafkaKeysResponse(
                    arbeidssoekerId.value,
                    kafkaKey.value
                )

                periodeTopic.pipeInput(arbeidssoekerId.value, startetPeriode)

                microfrontendTopic.isEmpty shouldBe false
                val keyValueList = microfrontendTopic.readKeyValuesToList()
                keyValueList.size shouldBe 2

                val minSideKeyValue = keyValueList.first()
                val behovsvurderingKeyValue = keyValueList.last()

                minSideKeyValue.key shouldBe arbeidssoekerId.value
                with(minSideKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.ENABLE
                    ident shouldBe startetPeriode.identitetsnummer
                    microfrontendId shouldBe applicationConfig.microfrontendToggle.aiaMinSide
                    sensitivitet shouldBe Sensitivitet.SUBSTANTIAL
                    initiatedBy shouldBe "paw"
                }

                behovsvurderingKeyValue.key shouldBe arbeidssoekerId.value
                with(behovsvurderingKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.ENABLE
                    ident shouldBe startetPeriode.identitetsnummer
                    microfrontendId shouldBe applicationConfig.microfrontendToggle.aiaBehovsvurdering
                    sensitivitet shouldBe Sensitivitet.HIGH
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

            "Skal aktivere behovsvurdering om periode er startet på deprekeringstidspunkt $deprekeringstidspunkt" {
                val kafkaKey = TestData.kafkaKey5
                val identitetsnummer = TestData.identitetsnummer5
                val arbeidssoekerId = TestData.arbeidsoekerId5
                val startetPeriode = buildPeriode(
                    id = UUID.randomUUID(),
                    identitetsnummer = identitetsnummer,
                    startetTidspunkt = deprekeringstidspunkt
                )

                coEvery { kafkaKeysClientMock.getIdAndKey(identitetsnummer.value) } returns KafkaKeysResponse(
                    arbeidssoekerId.value,
                    kafkaKey.value
                )

                periodeTopic.pipeInput(arbeidssoekerId.value, startetPeriode)

                microfrontendTopic.isEmpty shouldBe false
                val keyValueList = microfrontendTopic.readKeyValuesToList()
                keyValueList.size shouldBe 2

                val minSideKeyValue = keyValueList.first()
                val behovsvurderingKeyValue = keyValueList.last()

                minSideKeyValue.key shouldBe arbeidssoekerId.value
                with(minSideKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.ENABLE
                    ident shouldBe startetPeriode.identitetsnummer
                    microfrontendId shouldBe applicationConfig.microfrontendToggle.aiaMinSide
                    sensitivitet shouldBe Sensitivitet.SUBSTANTIAL
                    initiatedBy shouldBe "paw"
                }

                behovsvurderingKeyValue.key shouldBe arbeidssoekerId.value
                with(behovsvurderingKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.ENABLE
                    ident shouldBe startetPeriode.identitetsnummer
                    microfrontendId shouldBe applicationConfig.microfrontendToggle.aiaBehovsvurdering
                    sensitivitet shouldBe Sensitivitet.HIGH
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

            "Skal ikke aktivere behovsvurdering om periode er startet etter deprekeringstidspunkt $deprekeringstidspunkt" {
                val kafkaKey = TestData.kafkaKey5
                val identitetsnummer = TestData.identitetsnummer5
                val arbeidssoekerId = TestData.arbeidsoekerId5
                val startetPeriode = buildPeriode(
                    id = UUID.randomUUID(),
                    identitetsnummer = identitetsnummer,
                    startetTidspunkt = deprekeringstidspunkt.plusSeconds(60)
                )

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
                    microfrontendId shouldBe applicationConfig.microfrontendToggle.aiaMinSide
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
        }
    }
})

private class LocalTestContext(
    override val applicationConfig: ApplicationConfig
) : TestContext(applicationConfig = applicationConfig) {

    val testDriver = StreamsBuilder().apply {
        addStateStore(
            Stores.keyValueStoreBuilder(
                Stores.inMemoryKeyValueStore(applicationConfig.kafkaTopology.periodeStateStore),
                Serdes.Long(),
                periodeInfoSerde
            )
        )
        addPeriodeStream(
            applicationConfig,
            meterRegistry,
            kafkaKeysClientMock::getIdAndKeyBlocking
        )
    }.build()
        .let { TopologyTestDriver(it, kafkaStreamProperties) }

    val periodeStateStore =
        testDriver.getKeyValueStore<Long, PeriodeInfo>(applicationConfig.kafkaTopology.periodeStateStore)

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