package no.nav.paw.arbeidssoekerregisteret.topology

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.paw.arbeidssoekerregisteret.model.Beriket14aVedtak
import no.nav.paw.arbeidssoekerregisteret.model.PeriodeInfo
import no.nav.paw.arbeidssoekerregisteret.model.Toggle
import no.nav.paw.arbeidssoekerregisteret.model.ToggleAction
import no.nav.paw.arbeidssoekerregisteret.model.asPeriodeInfo
import no.nav.paw.arbeidssoekerregisteret.test.TestContext
import no.nav.paw.arbeidssoekerregisteret.test.TestData
import no.nav.paw.arbeidssoekerregisteret.test.addDeprekeringInMemoryStateStore
import no.nav.paw.arbeidssoekerregisteret.test.addPeriodeInMemoryStateStore
import no.nav.paw.arbeidssoekerregisteret.topology.streams.addBeriket14aVedtakStream
import no.nav.paw.arbeidssoekerregisteret.utils.getIdAndKeyBlocking
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.TestInputTopic
import org.apache.kafka.streams.TestOutputTopic
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.state.KeyValueStore

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
class Beriket14aVedtakStreamTest : FreeSpec({

    with(LocalTestContext()) {
        "Testsuite for toggling av AIA-microfrontends basert på beriket 14a vedtak" - {
            "Skal ikke deaktivere aia-behovsvurdering microfrontend om det ikke finnes noen periode tilhørende 14a vedtak" {
                val kafkaKey = TestData.kafkaKey1
                val beriket14aVedtak = TestData.beriket14aVedtak1

                beriket14aVedtakTopic.pipeInput(kafkaKey.value, beriket14aVedtak)

                microfrontendTopic.isEmpty shouldBe true
                periodeKeyValueStore.size() shouldBe 0
            }

            "Skal ikke deaktivere aia-behovsvurdering microfrontend om det ikke finnes en aktiv periode tilhørende 14a vedtak" {
                val kafkaKey = TestData.kafkaKey2
                val arbeidssoekerId = TestData.arbeidsoekerId2
                val avsluttetPeriode = TestData.periode2Avsluttet
                val beriket14aVedtak = TestData.beriket14aVedtak2

                periodeKeyValueStore.put(
                    arbeidssoekerId.value,
                    avsluttetPeriode.asPeriodeInfo(arbeidssoekerId.value)
                )

                beriket14aVedtakTopic.pipeInput(kafkaKey.value, beriket14aVedtak)

                microfrontendTopic.isEmpty shouldBe true
                periodeKeyValueStore.size() shouldBe 1
            }

            "Skal deaktivere aia-behovsvurdering microfrontend om det finnes en aktiv periode tilhørende 14a vedtak" {
                val kafkaKey = TestData.kafkaKey4
                val identitetsnummer = TestData.identitetsnummer4
                val arbeidssoekerId = TestData.arbeidsoekerId4
                val startetPeriode = TestData.periode4Startet
                val beriket14aVedtak = TestData.beriket14aVedtak4

                periodeKeyValueStore.put(
                    arbeidssoekerId.value,
                    startetPeriode.asPeriodeInfo(arbeidssoekerId.value)
                )

                beriket14aVedtakTopic.pipeInput(kafkaKey.value, beriket14aVedtak)

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

                periodeKeyValueStore.size() shouldBe 2
            }
        }
    }
}) {
    private class LocalTestContext : TestContext() {

        val testDriver = StreamsBuilder().apply {
            addDeprekeringInMemoryStateStore(applicationConfig)
            addPeriodeInMemoryStateStore(applicationConfig)
            addBeriket14aVedtakStream(
                applicationConfig,
                meterRegistry,
                kafkaKeysClientMock::getIdAndKeyBlocking
            )
        }.build()
            .let { TopologyTestDriver(it, kafkaStreamProperties) }


        val periodeKeyValueStore: KeyValueStore<Long, PeriodeInfo> = testDriver
            .getKeyValueStore(applicationConfig.kafkaTopology.periodeStateStore)

        val beriket14aVedtakTopic: TestInputTopic<Long, Beriket14aVedtak> = testDriver.createInputTopic(
            applicationConfig.kafkaTopology.beriket14aVedtakTopic,
            Serdes.Long().serializer(),
            beriket14aVedtakSerde.serializer()
        )

        val microfrontendTopic: TestOutputTopic<Long, Toggle> = testDriver.createOutputTopic(
            applicationConfig.kafkaTopology.microfrontendTopic,
            Serdes.Long().deserializer(),
            toggleSerde.deserializer()
        )
    }
}