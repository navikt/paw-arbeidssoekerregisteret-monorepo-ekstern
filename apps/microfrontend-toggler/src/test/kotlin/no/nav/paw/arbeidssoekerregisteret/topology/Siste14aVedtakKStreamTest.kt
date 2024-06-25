package no.nav.paw.arbeidssoekerregisteret.topology

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import no.nav.paw.arbeidssoekerregisteret.config.AppConfig
import no.nav.paw.arbeidssoekerregisteret.config.buildBeriket14aVedtakSerde
import no.nav.paw.arbeidssoekerregisteret.config.buildSiste14aVedtakSerde
import no.nav.paw.arbeidssoekerregisteret.context.ConfigContext
import no.nav.paw.arbeidssoekerregisteret.context.LoggingContext
import no.nav.paw.arbeidssoekerregisteret.model.Beriket14aVedtak
import no.nav.paw.arbeidssoekerregisteret.topology.streams.buildSiste14aVedtakKStream
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.TopologyTestDriver
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.*

class Siste14aVedtakKStreamTest : FreeSpec({

    with(TestContext()) {
        "Testsuite for berikelse av siste 14a vedtak" - {
            val aktivAktorId = "12345"
            val inaktivAktorId = "98765"
            val aktivArbeidsoekerId = 1234L
            val aktivKey = 23456L
            val aktivSiste14aVedtak = buildSiste14aVedtak(aktivAktorId, Instant.now())
            val inaktivSiste14aVedtak = buildSiste14aVedtak(inaktivAktorId, Instant.now().minus(Duration.ofDays(10)))
            every { kafkaKeysClientMock.hentKafkaKeys(aktivAktorId) } returns KafkaKeysResponse(
                aktivArbeidsoekerId,
                aktivKey
            )
            every { kafkaKeysClientMock.hentKafkaKeys(inaktivAktorId) } returns null

            "Skal ikke berike 14a vedtak om KafkaKeys returnerer null" {
                siste14aVedtakTopic.pipeInput(UUID.randomUUID().toString(), inaktivSiste14aVedtak)

                beriket14aVedtakTopic.isEmpty shouldBe true
            }

            "Skal ikke berike 14a vedtak" {
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
    private class TestContext {

        val appConfig = loadNaisOrLocalConfiguration<AppConfig>(TEST_APPLICATION_CONFIG_FILE_NAME)
        val logger: Logger = LoggerFactory.getLogger("TestApplication")
        val auditLogger: Logger = LoggerFactory.getLogger("TestAudit")
        val meterRegistry = SimpleMeterRegistry()
        val siste14aVedtakSerde = buildSiste14aVedtakSerde()
        val beriket14aVedtakSerde = buildBeriket14aVedtakSerde()
        val kafkaKeysClientMock = mockk<KafkaKeysClientMock>()

        val testDriver =
            with(ConfigContext(appConfig)) {
                with(LoggingContext(logger, auditLogger)) {
                    StreamsBuilder().apply {
                        buildSiste14aVedtakKStream(
                            meterRegistry,
                            kafkaKeysClientMock::hentKafkaKeys
                        )
                    }.build()
                }
            }.let { TopologyTestDriver(it, kafkaStreamProperties) }

        val siste14aVedtakTopic = testDriver.createInputTopic(
            appConfig.kafkaStreams.siste14aVedtakTopic,
            Serdes.String().serializer(),
            siste14aVedtakSerde.serializer()
        )

        val beriket14aVedtakTopic = testDriver.createOutputTopic(
            appConfig.kafkaStreams.beriket14aVedtakTopic,
            Serdes.Long().deserializer(),
            beriket14aVedtakSerde.deserializer()
        )
    }
}