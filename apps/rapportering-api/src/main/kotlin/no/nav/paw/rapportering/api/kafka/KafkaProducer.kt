package no.nav.paw.rapportering.api.kafka

import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.config.kafka.KafkaFactory
import no.nav.paw.rapportering.api.config.ApplicationConfig
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.LongSerializer
import org.apache.kafka.common.serialization.Serializer

class KafkaProducer<T : SpecificRecord>(
    private val kafkaConfig: KafkaConfig,
    private val applicationConfig: ApplicationConfig,
    private val serializer: Serializer<T>
) {
    private lateinit var producer: Producer<Long, T>

    init {
        initializeProducer()
    }

    private fun initializeProducer() {
        val kafkaFactory = KafkaFactory(kafkaConfig)
        producer =
            kafkaFactory.createProducer<Long, T>(
                clientId = applicationConfig.applicationIdSuffix, //TODO: clientId
                keySerializer = LongSerializer::class,
                valueSerializer = serializer::class
            )
    }

    fun produceMessage(topic: String, key: Long, message: T) {
        producer.send(ProducerRecord(topic, key, message))
    }

    fun closeProducer() {
        producer.close()
    }
}