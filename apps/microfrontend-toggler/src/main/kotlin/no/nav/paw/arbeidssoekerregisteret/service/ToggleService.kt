package no.nav.paw.arbeidssoekerregisteret.service

import no.nav.paw.arbeidssoekerregisteret.context.ConfigContext
import no.nav.paw.arbeidssoekerregisteret.context.LoggingContext
import no.nav.paw.arbeidssoekerregisteret.model.Toggle
import no.nav.paw.arbeidssoekerregisteret.model.ToggleAction
import no.nav.paw.arbeidssoekerregisteret.model.buildDisableToggle
import no.nav.paw.arbeidssoekerregisteret.model.buildEnableToggle
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord

class ToggleService(
    private val kafkaKeysClient: KafkaKeysClient,
    private val kafkaProducer: Producer<Long, Toggle>
) {

    context(ConfigContext, LoggingContext)
    suspend fun sendToggle(action: ToggleAction, identitetsnummer: String, microfrontendId: String): Toggle {
        val kafkaKeysResponse = kafkaKeysClient.getIdAndKey(identitetsnummer)
        val arbeidssoekerId = checkNotNull(kafkaKeysResponse?.id) { "KafkaKeysResponse er null" }

        val toggle: Toggle = when (action) {
            ToggleAction.ENABLE -> {
                logger.debug("Iverksetter aktivering av {}.", microfrontendId)
                buildEnableToggle(identitetsnummer, microfrontendId)
            }

            ToggleAction.DISABLE -> {
                logger.debug("Iverksetter deaktivering av {}.", microfrontendId)
                buildDisableToggle(identitetsnummer, microfrontendId)
            }
        }

        // TODO: Trengs det å slette innslag i state store?
        kafkaProducer.send(ProducerRecord(appConfig.kafkaStreams.microfrontendTopic, arbeidssoekerId, toggle))

        return toggle
    }
}