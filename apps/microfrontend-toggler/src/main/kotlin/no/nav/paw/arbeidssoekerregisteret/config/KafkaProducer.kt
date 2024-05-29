package no.nav.paw.arbeidssoekerregisteret.config

import no.nav.paw.arbeidssoekerregisteret.model.Toggle
import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.config.kafka.KafkaFactory
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.common.serialization.LongSerializer

fun buildToggleKafkaProducer(kafkaConfig: KafkaConfig): Producer<Long, Toggle> {
    val kafkaFactory = KafkaFactory(kafkaConfig)
    return kafkaFactory.createProducer(
        clientId = "${kafkaConfig.applicationIdPrefix}_$KAFKA_PRODUCER_APPLICATION_ID_SUFFIX",
        keySerializer = LongSerializer::class,
        valueSerializer = ToggleJsonSerializer::class
    )
}