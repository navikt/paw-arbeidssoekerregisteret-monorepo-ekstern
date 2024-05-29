package no.nav.paw.arbeidssoekerregisteret.service

import no.nav.paw.arbeidssoekerregisteret.context.ConfigContext
import no.nav.paw.arbeidssoekerregisteret.model.Toggle
import no.nav.paw.arbeidssoekerregisteret.model.ToggleAction
import no.nav.paw.arbeidssoekerregisteret.model.ToggleRequest
import no.nav.paw.arbeidssoekerregisteret.model.buildDisableToggle
import no.nav.paw.arbeidssoekerregisteret.model.buildEnableToggle
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord

class ToggleService(
    private val kafkaKeysClient: KafkaKeysClient,
    private val kafkaProducer: Producer<Long, Toggle>
) {

    context(ConfigContext)
    suspend fun processToggle(toggleRequest: ToggleRequest): Toggle {
        with(toggleRequest) {
            val kafkaKeysResponse = kafkaKeysClient.getIdAndKey(ident)
            val arbeidssoekerId = checkNotNull(kafkaKeysResponse?.id) { "KafkaKeysResponse er null" }

            val toggle: Toggle = when (action) {
                ToggleAction.ENABLE -> buildEnableToggle(ident, microfrontendId)
                ToggleAction.DISABLE -> buildDisableToggle(ident, microfrontendId)
            }

            kafkaProducer.send(ProducerRecord(appConfig.kafkaTopology.microfrontendTopic, arbeidssoekerId, toggle))

            return toggle
        }
    }
}