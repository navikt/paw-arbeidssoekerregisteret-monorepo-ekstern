package no.nav.paw.arbeidssoekerregisteret.service

import no.nav.paw.arbeidssoekerregisteret.context.ConfigContext
import no.nav.paw.arbeidssoekerregisteret.context.LoggingContext
import no.nav.paw.arbeidssoekerregisteret.model.Toggle
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord

class ToggleService(
    private val kafkaKeysClient: KafkaKeysClient,
    private val kafkaProducer: Producer<Long, Toggle>
) {

    context(ConfigContext, LoggingContext)
    suspend fun sendToggle(toggle: Toggle) {
        val kafkaKeysResponse = kafkaKeysClient.getIdAndKey(toggle.ident)
        val arbeidssoekerId = checkNotNull(kafkaKeysResponse?.id) { "KafkaKeysResponse er null" }

        // TODO: Trengs det å slette innslag i state store?
        kafkaProducer.send(ProducerRecord(appConfig.kafkaStreams.microfrontendTopic, arbeidssoekerId, toggle))
    }
}