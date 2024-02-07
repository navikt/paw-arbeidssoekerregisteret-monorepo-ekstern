package no.nav.arbeidssokerregisteret.arena.adapter

import no.nav.paw.arbeidssokerregisteret.arena.adapter.config.ApplicationConfig
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.config.kafka.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.config.kafka.KafkaFactory
import no.nav.paw.config.kafka.streams.KafkaStreamsFactory
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.LongSerializer

fun main() {
    val kafkaStreamsConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG_WITH_SCHEME_REG)
    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>("application.toml")

    val (topics) = applicationConfig

    val localTestProducer = LocalTestProducer(kafkaStreamsConfig)

    val testDataList = listOf(
        TestData.opplysningerOmArbeidssoeker to topics.opplysningerOmArbeidssoeker,
        TestData.perioder to topics.arbeidssokerperioder,
        TestData.profilering to topics.profilering
    )

    testDataList.forEach { (testData, topic) ->
        localTestProducer.produceMessage(testData, topic)
    }
}

class LocalTestProducer(
    private val kafkaStreamsConfig: KafkaConfig
) {
    fun <T : SpecificRecord> produceMessage(testData: List<T>, topic: String) {
        val streamsFactory = KafkaStreamsFactory("test", kafkaStreamsConfig)
        KafkaFactory(kafkaStreamsConfig)
            .createProducer(
                clientId = "paw-arbeidssokerregisteret-arena-adapter",
                keySerializer = LongSerializer::class,
                valueSerializer = streamsFactory.createSpecificAvroSerde<T>().serializer()::class
            ).use { producer ->
                testData.forEachIndexed { i, message ->
                    try {
                        val record =
                            ProducerRecord(topic, i.toLong(), message)
                        producer.send(record).get().also {
                            println("Sent $i")
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
    }
}