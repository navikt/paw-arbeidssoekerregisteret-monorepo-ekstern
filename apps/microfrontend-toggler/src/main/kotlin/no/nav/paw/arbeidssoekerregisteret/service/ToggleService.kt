package no.nav.paw.arbeidssoekerregisteret.service

import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.model.Toggle
import no.nav.paw.arbeidssoekerregisteret.model.ToggleAction
import no.nav.paw.arbeidssoekerregisteret.utils.ToggleJsonSerializer
import no.nav.paw.arbeidssoekerregisteret.utils.buildLogger
import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.config.kafka.KafkaFactory
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.LongSerializer

class ToggleService(
    private val applicationConfig: ApplicationConfig,
    private val kafkaConfig: KafkaConfig,
    private val kafkaKeysFunction: suspend (ident: String) -> KafkaKeysResponse
) {
    private val logger = buildLogger
    private var producer: Producer<Long, Toggle>

    init {
        val kafkaFactory = KafkaFactory(kafkaConfig)
        producer = kafkaFactory.createProducer(
            clientId = "${kafkaConfig.applicationIdPrefix}_${applicationConfig.kafkaTopology.toggleProducerIdSuffix}",
            keySerializer = LongSerializer::class,
            valueSerializer = ToggleJsonSerializer::class
        )
    }

    suspend fun sendToggle(toggle: Toggle) {
        val kafkaKeysResponse = kafkaKeysFunction(toggle.ident)
        val arbeidssoekerId = kafkaKeysResponse.id

        if (toggle.action == ToggleAction.ENABLE) {
            logger.info("Mottok ekstern forespørsel om aktivering av {}.", toggle.microfrontendId)
        } else {
            logger.info("Mottok ekstern forespørsel om deaktivering av {}.", toggle.microfrontendId)
        }

        producer.send(ProducerRecord(applicationConfig.kafkaTopology.microfrontendTopic, arbeidssoekerId, toggle))
    }

    fun closeKafkaProducer() {
        producer.close()
    }

    fun getKafkaMetricsBinder() = KafkaClientMetrics(producer)
}