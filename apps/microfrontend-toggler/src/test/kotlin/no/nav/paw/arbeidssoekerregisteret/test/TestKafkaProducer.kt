package no.nav.paw.arbeidssoekerregisteret.test

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerializer
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.standardTopicNames
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.kafka.config.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.factory.KafkaFactory
import no.nav.paw.logging.logger.buildApplicationLogger
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.serialization.LongSerializer

private val logger = buildApplicationLogger

fun main() {
    val topicNames = standardTopicNames(currentRuntimeEnvironment)
    val producerIdPrefix = "paw-microfrontend-toggler"
    val periodeProducerId = "$producerIdPrefix-periode-producer"
    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG_WITH_SCHEME_REG)

    val periodeKafkaProducer = buildKafkaProducer(
        periodeProducerId,
        kafkaConfig,
        PeriodeSerializer()
    )

    val perioder = mapOf(
        TestData.kafkaKey1.value to TestData.periode1Startet,
        TestData.kafkaKey2.value to TestData.periode2Startet,
        //TestData.kafkaKey1.value to TestData.periode1Avsluttet,
    )

    try {
        perioder.forEach { (key, value) ->
            logger.info("Sender periode {}", value.id)
            periodeKafkaProducer.sendRecord(topicNames.periodeTopic, key, value)
        }
    } catch (e: Exception) {
        logger.error("Send periode feilet", e)
    } finally {
        periodeKafkaProducer.close()
    }
}

private fun <T : SpecificRecord> Producer<Long, T>.sendRecord(
    topic: String,
    key: Long,
    value: T
): RecordMetadata? {
    return send(ProducerRecord(topic, key, value)).get()
}

private fun <T : SpecificRecord> buildKafkaProducer(
    producerId: String,
    kafkaConfig: KafkaConfig,
    valueSerializer: SpecificAvroSerializer<T>
): Producer<Long, T> {
    val kafkaFactory = KafkaFactory(kafkaConfig)

    return kafkaFactory.createProducer<Long, T>(
        clientId = producerId,
        keySerializer = LongSerializer::class,
        valueSerializer = valueSerializer::class
    )
}

class PeriodeSerializer : SpecificAvroSerializer<Periode>()
