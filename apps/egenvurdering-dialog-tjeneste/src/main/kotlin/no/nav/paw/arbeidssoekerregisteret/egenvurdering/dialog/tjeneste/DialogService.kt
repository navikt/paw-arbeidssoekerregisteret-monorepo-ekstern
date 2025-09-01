package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste

import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.client.VeilarbdialogClient
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.utils.buildApplicationLogger
import no.nav.paw.arbeidssokerregisteret.api.v2.Egenvurdering
import org.apache.kafka.clients.consumer.ConsumerRecords

private val logger = buildApplicationLogger

class DialogService(val veilarbdialogClient: VeilarbdialogClient) {
    fun handleRecords(records: ConsumerRecords<Long, Egenvurdering>) {
        logger.info("Received ${records.count()} records from Kafka")
        records.forEach { record ->
            val egenvurdering = record.value()
            val dialogmelding = egenvurdering.tilDialogmelding()
            //val response = runBlocking { veilarbdialogClient.lagEllerOppdaterDialog(dialogmelding.toDialogRequest()) }
            TODO("handle response")
        }
    }
}
