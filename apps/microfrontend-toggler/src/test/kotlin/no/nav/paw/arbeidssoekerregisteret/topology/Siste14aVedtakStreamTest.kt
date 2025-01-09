package no.nav.paw.arbeidssoekerregisteret.topology

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import no.nav.paw.arbeidssoekerregisteret.model.Beriket14aVedtak
import no.nav.paw.arbeidssoekerregisteret.test.TestContext
import no.nav.paw.arbeidssoekerregisteret.test.TestData
import no.nav.paw.arbeidssoekerregisteret.topology.streams.buildSiste14aVedtakStream
import no.nav.paw.arbeidssoekerregisteret.utils.getIdAndKeyOrNullBlocking
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.TopologyTestDriver
import java.time.Duration
import java.time.Instant
import java.util.*

class Siste14aVedtakStreamTest : FreeSpec({

    with(LocalTestContext()) {
        "Testsuite for berikelse av siste 14a vedtak" - {
            val aktivAktorId = TestData.aktorId1
            val inaktivAktorId = TestData.aktorId2
            val aktivArbeidsoekerId = TestData.arbeidsoekerId1
            val aktivKey = TestData.kafkaKey1
            val aktivSiste14aVedtak = TestData.buildSiste14aVedtak(aktivAktorId, Instant.now())
            val inaktivSiste14aVedtak = TestData
                .buildSiste14aVedtak(inaktivAktorId, Instant.now().minus(Duration.ofDays(10)))

            "Skal ikke berike 14a vedtak om KafkaKeys returnerer null" {
                coEvery { kafkaKeysClientMock.getIdAndKeyOrNullBlocking(any<String>()) } returns null

                siste14aVedtakTopic.pipeInput(UUID.randomUUID().toString(), inaktivSiste14aVedtak)

                beriket14aVedtakTopic.isEmpty shouldBe true
            }

            "Skal ikke berike 14a vedtak" {
                coEvery { kafkaKeysClientMock.getIdAndKeyOrNullBlocking(any<String>()) } returns KafkaKeysResponse(
                    aktivArbeidsoekerId,
                    aktivKey
                )

                siste14aVedtakTopic.pipeInput(UUID.randomUUID().toString(), aktivSiste14aVedtak)

                beriket14aVedtakTopic.isEmpty shouldBe false
                val keyValueList = beriket14aVedtakTopic.readKeyValuesToList()
                keyValueList.size shouldBe 1

                val beriket14aVedtakKeyValue = keyValueList.last()

                beriket14aVedtakKeyValue.key shouldBe aktivKey
                with(beriket14aVedtakKeyValue.value.shouldBeInstanceOf<Beriket14aVedtak>()) {
                    aktorId shouldBe aktivSiste14aVedtak.aktorId.get()
                    arbeidssoekerId shouldBe aktivArbeidsoekerId
                    innsatsgruppe shouldBe aktivSiste14aVedtak.innsatsgruppe
                    hovedmal shouldBe aktivSiste14aVedtak.hovedmal
                    fattetDato shouldBe aktivSiste14aVedtak.fattetDato
                    fraArena shouldBe aktivSiste14aVedtak.fraArena
                }
            }
        }
    }
}) {
    private class LocalTestContext : TestContext() {

        val testDriver = StreamsBuilder().apply {
            buildSiste14aVedtakStream(
                applicationConfig,
                meterRegistry,
                kafkaKeysClientMock::getIdAndKeyOrNullBlocking
            )
        }.build().let { TopologyTestDriver(it, kafkaStreamProperties) }

        val siste14aVedtakTopic = testDriver.createInputTopic(
            applicationConfig.kafkaTopology.siste14aVedtakTopic,
            Serdes.String().serializer(),
            siste14aVedtakSerde.serializer()
        )

        val beriket14aVedtakTopic = testDriver.createOutputTopic(
            applicationConfig.kafkaTopology.beriket14aVedtakTopic,
            Serdes.Long().deserializer(),
            beriket14aVedtakSerde.deserializer()
        )
    }
}