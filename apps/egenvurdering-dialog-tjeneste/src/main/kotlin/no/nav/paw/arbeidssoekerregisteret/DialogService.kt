package no.nav.paw.arbeidssoekerregisteret

import no.nav.paw.arbeidssoekerregisteret.utils.buildApplicationLogger
import no.nav.paw.arbeidssokerregisteret.api.v2.Egenvurdering
import org.apache.kafka.clients.consumer.ConsumerRecords

private val logger = buildApplicationLogger

class DialogService {
    fun handleRecords(records: ConsumerRecords<Long, Egenvurdering>) {
        logger.info("Received ${records.count()} records from Kafka")
    }
}
