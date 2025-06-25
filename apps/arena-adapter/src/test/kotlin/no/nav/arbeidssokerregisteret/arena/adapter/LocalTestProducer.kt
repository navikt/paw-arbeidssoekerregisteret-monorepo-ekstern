package no.nav.arbeidssokerregisteret.arena.adapter

import no.nav.paw.arbeidssokerregisteret.arena.adapter.config.ApplicationConfig
import no.nav.paw.arbeidssokerregisteret.arena.adapter.config.Topics
import no.nav.paw.arbeidssokerregisteret.standardTopicNames
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.kafka.config.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.factory.KafkaFactory
import no.nav.paw.kafka.factory.KafkaStreamsFactory
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.LongSerializer

fun main() {
    val kafkaStreamsConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG_WITH_SCHEME_REG)
    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>("application.toml")

    val (_, arenaTopics) = applicationConfig
    val topics = Topics(
        arenaTopics = arenaTopics,
        standardTopics = standardTopicNames(currentRuntimeEnvironment)
    )

    val localTestProducer = LocalTestProducer(kafkaStreamsConfig)

    val testDataList = listOf(
        TestData.opplysningerOmArbeidssoeker to topics.opplysningerOmArbeidssoeker,
        TestData.perioder to topics.arbeidssokerperioder,
        TestData.profilering to topics.profilering
    )

    testDataList.forEach { (testData, topic) ->
        localTestProducer.produceMessage(testData, topic)
    }
    testDataList.forEach { (testData, topic) ->
        localTestProducer.produceMessage(testData, topic)
    }
}

class LocalTestProducer(
    private val kafkaStreamsConfig: KafkaConfig
) {
    fun <T : SpecificRecord> produceMessage(testData: List<Pair<Long, T>>, topic: String) {
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
                            ProducerRecord(topic, message.first, message.second)
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