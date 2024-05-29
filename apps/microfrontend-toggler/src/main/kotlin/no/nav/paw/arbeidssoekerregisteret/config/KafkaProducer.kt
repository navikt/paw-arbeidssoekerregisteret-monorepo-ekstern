package no.nav.paw.arbeidssoekerregisteret.config

import no.nav.paw.arbeidssoekerregisteret.model.Toggle
import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.config.kafka.KafkaFactory
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.common.serialization.LongSerializer

fun buildToggleKafkaProducer(
    config: KafkaConfig,
    producerConfig: KafkaProducerConfig
): Producer<Long, Toggle> {
    val kafkaFactory = KafkaFactory(config)
    return kafkaFactory.createProducer(
        clientId = "${config.applicationIdPrefix}_${producerConfig.applicationIdSuffix}",
        keySerializer = LongSerializer::class,
        valueSerializer = ToggleJsonSerializer::class
    )
}