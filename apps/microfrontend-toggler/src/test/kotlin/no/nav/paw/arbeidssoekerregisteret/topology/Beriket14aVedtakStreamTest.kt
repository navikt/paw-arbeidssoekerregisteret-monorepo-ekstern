package no.nav.paw.arbeidssoekerregisteret.topology

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.paw.arbeidssoekerregisteret.model.PeriodeInfo
import no.nav.paw.arbeidssoekerregisteret.model.Toggle
import no.nav.paw.arbeidssoekerregisteret.model.ToggleAction
import no.nav.paw.arbeidssoekerregisteret.model.buildPeriodeInfo
import no.nav.paw.arbeidssoekerregisteret.test.TestContext
import no.nav.paw.arbeidssoekerregisteret.test.TestData
import no.nav.paw.arbeidssoekerregisteret.topology.streams.buildBeriket14aVedtakStream
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.state.Stores
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
class Beriket14aVedtakStreamTest : FreeSpec({

    with(LocalTestContext()) {
        "Testsuite for toggling av AIA-microfrontends basert på beriket 14a vedtak" - {
            val periodeAvsluttetTidspunkt = Instant.now()
            val periodeStartTidspunkt = periodeAvsluttetTidspunkt.minus(Duration.ofDays(10))
            val startetPeriode = TestData.buildPeriode(
                startetTidspunkt = periodeStartTidspunkt
            )
            val avsluttetPeriode = TestData.buildPeriode(
                startetTidspunkt = periodeStartTidspunkt,
                avsluttetTidspunkt = periodeAvsluttetTidspunkt
            )
            val beriket14aVedtak = TestData
                .buildBeriket14aVedtak(fattetDato = periodeStartTidspunkt.plus(Duration.ofDays(2)))

            "Skal ikke deaktivere aia-behovsvurdering microfrontend om det ikke finnes noen periode tilhørende 14a vedtak" {
                beriket14aVedtakTopic.pipeInput(TestData.kafkaKey1, beriket14aVedtak)

                microfrontendTopic.isEmpty shouldBe true
                periodeKeyValueStore.size() shouldBe 0
            }

            "Skal ikke deaktivere aia-behovsvurdering microfrontend om det ikke finnes en aktiv periode tilhørende 14a vedtak" {
                periodeKeyValueStore.put(
                    TestData.arbeidsoekerId1,
                    avsluttetPeriode.buildPeriodeInfo(TestData.arbeidsoekerId1)
                )

                beriket14aVedtakTopic.pipeInput(TestData.kafkaKey1, beriket14aVedtak)

                microfrontendTopic.isEmpty shouldBe true
                periodeKeyValueStore.size() shouldBe 1
            }

            "Skal deaktivere aia-behovsvurdering microfrontend om det finnes en aktiv periode tilhørende 14a vedtak" {
                periodeKeyValueStore.put(
                    TestData.arbeidsoekerId1,
                    startetPeriode.buildPeriodeInfo(TestData.arbeidsoekerId1)
                )

                beriket14aVedtakTopic.pipeInput(TestData.kafkaKey1, beriket14aVedtak)

                microfrontendTopic.isEmpty shouldBe false
                val keyValueList = microfrontendTopic.readKeyValuesToList()
                keyValueList.size shouldBe 1

                val behovsvurderingKeyValue = keyValueList.last()

                behovsvurderingKeyValue.key shouldBe TestData.arbeidsoekerId1
                with(behovsvurderingKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.DISABLE
                    ident shouldBe avsluttetPeriode.identitetsnummer
                    microfrontendId shouldBe applicationConfig.microfrontendToggle.aiaBehovsvurdering
                    sensitivitet shouldBe null
                    initialedBy shouldBe "paw"
                }

                periodeKeyValueStore.size() shouldBe 1
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
            buildBeriket14aVedtakStream(applicationConfig, meterRegistry)
        }.build()
            .let { TopologyTestDriver(it, kafkaStreamProperties) }


        val periodeKeyValueStore =
            testDriver.getKeyValueStore<Long, PeriodeInfo>(applicationConfig.kafkaTopology.periodeStoreName)

        val beriket14aVedtakTopic = testDriver.createInputTopic(
            applicationConfig.kafkaTopology.beriket14aVedtakTopic,
            Serdes.Long().serializer(),
            beriket14aVedtakSerde.serializer()
        )

        val microfrontendTopic = testDriver.createOutputTopic(
            applicationConfig.kafkaTopology.microfrontendTopic,
            Serdes.Long().deserializer(),
            toggleSerde.deserializer()
        )
    }
}