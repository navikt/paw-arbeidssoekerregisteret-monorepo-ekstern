package no.nav.paw.arbeidssoekerregisteret.topology

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import no.nav.paw.arbeidssoekerregisteret.config.APPLICATION_CONFIG
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.model.DeprekeringStatus
import no.nav.paw.arbeidssoekerregisteret.model.Toggle
import no.nav.paw.arbeidssoekerregisteret.model.ToggleAction
import no.nav.paw.arbeidssoekerregisteret.test.TestContext
import no.nav.paw.arbeidssoekerregisteret.test.TestData
import no.nav.paw.arbeidssoekerregisteret.test.addDeprekeringInMemoryStateStore
import no.nav.paw.arbeidssoekerregisteret.test.addPeriodeInMemoryStateStore
import no.nav.paw.arbeidssoekerregisteret.topology.streams.addBeriket14aVedtakStream
import no.nav.paw.arbeidssoekerregisteret.utils.getIdAndKeyBlocking
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.TestOutputTopic
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.state.KeyValueStore
import java.time.Duration
import kotlin.io.path.toPath

class DeprekeringStreamTest : FreeSpec({
    val applicationConfig: ApplicationConfig = loadNaisOrLocalConfiguration(APPLICATION_CONFIG)

    "Testsuite for deprekering/deaktivering av behovsvurdering" - {
        "Skal ikke deaktivere aia-behovsvurdering microfrontends om jobben er deaktivert" {
            val justertApplicationConfig = applicationConfig.copy(
                deprekering = applicationConfig.deprekering.copy(
                    aktivert = false
                )
            )
            with(LocalTestContext(applicationConfig = justertApplicationConfig)) {
                microfrontendTopic.isEmpty shouldBe true
                deprekeringStateStore.size() shouldBe 0

                testDriver.advanceWallClockTime(Duration.ofHours(2))

                microfrontendTopic.isEmpty shouldBe true
                deprekeringStateStore.size() shouldBe 0
            }
        }

        "Skal ikke deaktivere aia-behovsvurdering microfrontends om jobben kjører på feil partisjon" {
            val justertApplicationConfig = applicationConfig.copy(
                deprekering = applicationConfig.deprekering.copy(
                    punctuatorActivePartition = 100
                )
            )
            with(LocalTestContext(applicationConfig = justertApplicationConfig)) {
                microfrontendTopic.isEmpty shouldBe true
                deprekeringStateStore.size() shouldBe 0

                testDriver.advanceWallClockTime(Duration.ofHours(2))

                microfrontendTopic.isEmpty shouldBe true
                deprekeringStateStore.size() shouldBe 0
            }
        }

        "Skal ikke deaktivere aia-behovsvurdering microfrontends om det ikke finnes noen CSV-fil" {
            val justertApplicationConfig = applicationConfig.copy(
                deprekering = applicationConfig.deprekering.copy(
                    csvFil = "/tmp/missing.csv"
                )
            )
            with(LocalTestContext(applicationConfig = justertApplicationConfig)) {
                microfrontendTopic.isEmpty shouldBe true
                deprekeringStateStore.size() shouldBe 0

                testDriver.advanceWallClockTime(Duration.ofHours(2))

                microfrontendTopic.isEmpty shouldBe true
                deprekeringStateStore.size() shouldBe 0
            }
        }

        "Skal ikke deaktivere aia-behovsvurdering microfrontends om det finnes en tom CSV-fil" {
            val filePath = javaClass.getResource("/csv/empty.csv").toURI().toPath()
            val justertApplicationConfig = applicationConfig.copy(
                deprekering = applicationConfig.deprekering.copy(
                    csvFil = filePath.toString()
                )
            )
            with(LocalTestContext(applicationConfig = justertApplicationConfig)) {
                microfrontendTopic.isEmpty shouldBe true
                deprekeringStateStore.size() shouldBe 0

                testDriver.advanceWallClockTime(Duration.ofHours(2))

                microfrontendTopic.isEmpty shouldBe true
                deprekeringStateStore.size() shouldBe 0
            }
        }

        "Skal deaktivere aia-behovsvurdering microfrontends for CSV-fil" {
            val filePath = javaClass.getResource("/csv/data.csv").toURI().toPath()
            val justertApplicationConfig = applicationConfig.copy(
                deprekering = applicationConfig.deprekering.copy(
                    csvFil = filePath.toString()
                )
            )
            with(LocalTestContext(applicationConfig = justertApplicationConfig)) {
                coEvery { kafkaKeysClientMock.getIdAndKey(TestData.identitetsnummer1.value) } returns TestData.kafkaKeysResponse1
                coEvery { kafkaKeysClientMock.getIdAndKey(TestData.identitetsnummer2.value) } returns TestData.kafkaKeysResponse2
                coEvery { kafkaKeysClientMock.getIdAndKey(TestData.identitetsnummer3.value) } returns TestData.kafkaKeysResponse3
                coEvery { kafkaKeysClientMock.getIdAndKey(TestData.identitetsnummer4.value) } returns TestData.kafkaKeysResponse4
                coEvery { kafkaKeysClientMock.getIdAndKey(TestData.identitetsnummer5.value) } returns TestData.kafkaKeysResponse5
                val identer = mapOf(
                    TestData.arbeidsoekerId1 to TestData.identitetsnummer1,
                    TestData.arbeidsoekerId2 to TestData.identitetsnummer2,
                    TestData.arbeidsoekerId3 to TestData.identitetsnummer3,
                    TestData.arbeidsoekerId4 to TestData.identitetsnummer4,
                    TestData.arbeidsoekerId5 to TestData.identitetsnummer5
                )
                val expectedToggleList = identer
                    .mapValues { (_, identitetsnummer) ->
                        Toggle(
                            action = ToggleAction.DISABLE,
                            ident = identitetsnummer.value,
                            microfrontendId = applicationConfig.microfrontendToggle.aiaBehovsvurdering,
                            initiatedBy = "paw"
                        )
                    }.map { (arbeidssoekerId, toggle) -> KeyValue(arbeidssoekerId.value, toggle) }
                val expectedStateStoreList = identer
                    .map { (_, identitetsnummer) -> KeyValue(identitetsnummer.value, DeprekeringStatus.DISABLED.name) }

                microfrontendTopic.isEmpty shouldBe true
                deprekeringStateStore.size() shouldBe 0

                testDriver.advanceWallClockTime(Duration.ofHours(2))

                microfrontendTopic.isEmpty shouldBe false
                val keyValueList1 = microfrontendTopic.readKeyValuesToList()
                keyValueList1 shouldContainOnly expectedToggleList
                val stateStoreList1 = deprekeringStateStore.asList()
                stateStoreList1 shouldContainOnly expectedStateStoreList

                testDriver.advanceWallClockTime(Duration.ofHours(2))

                microfrontendTopic.isEmpty shouldBe true
                val keyValueList2 = microfrontendTopic.readKeyValuesToList()
                keyValueList2 shouldHaveSize 0
                val stateStoreList2 = deprekeringStateStore.asList()
                stateStoreList2 shouldContainOnly expectedStateStoreList
            }
        }
    }
}) {
    private class LocalTestContext(
        override val applicationConfig: ApplicationConfig
    ) : TestContext(applicationConfig = applicationConfig) {

        val testDriver = StreamsBuilder().apply {
            addDeprekeringInMemoryStateStore(applicationConfig)
            addPeriodeInMemoryStateStore(applicationConfig)
            addBeriket14aVedtakStream(
                applicationConfig,
                meterRegistry,
                kafkaKeysClientMock::getIdAndKeyBlocking
            )
        }.build().let { TopologyTestDriver(it, kafkaStreamProperties) }

        val deprekeringStateStore: KeyValueStore<String, String> = testDriver
            .getKeyValueStore(applicationConfig.deprekering.stateStore)

        val microfrontendTopic: TestOutputTopic<Long, Toggle> = testDriver.createOutputTopic(
            applicationConfig.kafkaTopology.microfrontendTopic,
            Serdes.Long().deserializer(),
            toggleSerde.deserializer()
        )
    }
}