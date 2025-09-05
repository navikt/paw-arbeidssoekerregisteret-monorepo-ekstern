package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste

import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import kotlinx.coroutines.runBlocking
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.client.ArbeidsoppfølgingsperiodeAvsluttet
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.client.DialogResponse
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.client.DialogResultat
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.client.VeilarbdialogClient
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.client.toDialogRequest
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.repository.PeriodeIdDialogIdRepository
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.utils.buildApplicationLogger
import no.nav.paw.arbeidssokerregisteret.api.v2.Egenvurdering
import org.apache.kafka.clients.consumer.ConsumerRecords
import java.util.*

private val logger = buildApplicationLogger

class DialogService(
    private val veilarbdialogClient: VeilarbdialogClient,
    private val periodeIdDialogIdRepository: PeriodeIdDialogIdRepository = PeriodeIdDialogIdRepository,
) {
    fun varsleVeilederOmEgenvurderingAvProfilering(records: ConsumerRecords<Long, Egenvurdering>) {
        records.asSequence()
            .map { it.value() }
            .map { egenvurdering ->
                val eksisterendeDialogId: Long? = periodeIdDialogIdRepository.getDialogIdOrNull(egenvurdering.periodeId)
                val dialogmelding = egenvurdering.tilDialogmelding()
                val dialogRequest =
                    dialogmelding.toDialogRequest(egenvurdering.sendtInnAv.utfoertAv.id, eksisterendeDialogId)
                val response = runBlocking { veilarbdialogClient.lagEllerOppdaterDialog(dialogRequest) }
                TempData(egenvurdering.periodeId, eksisterendeDialogId, response)
            }.forEach { (periodeId, eksisterendeDialogId, response) ->
                when (response) {
                    is DialogResponse -> {
                        if (eksisterendeDialogId == null) {
                            periodeIdDialogIdRepository.insert(periodeId, response.dialogId.toLong())
                        } else if (eksisterendeDialogId != response.dialogId.toLong()) {
                            traceEndringAvDialogId(eksisterendeDialogId, response, periodeId)
                            //TODO: Edgecase
                            //periodeIdDialogIdRepository.update(egenvurdering.periodeId, response.dialogId.toLong())
                        }
                    }

                    is ArbeidsoppfølgingsperiodeAvsluttet -> {
                        traceArbeidsoppfølgingsperiodeAvsluttet(periodeId, eksisterendeDialogId)
                    }
                }
            }
    }

    private fun traceEndringAvDialogId(
        dialogId: Long,
        response: DialogResponse,
        periodeId: UUID,
    ) {
        Span.current().addEvent(
            "endret_dialog_id", Attributes.of(
                stringKey("eksisterende_dialogId"), dialogId.toString(),
                stringKey("ny_dialogId"), response.dialogId
            )
        )
        logger.warn("Fant ikke dialog med $dialogId. Oppdaterer til ny dialogId=${response.dialogId}, for periodeId=$periodeId.")
    }

    private fun traceArbeidsoppfølgingsperiodeAvsluttet(periodeId: UUID, dialogId: Long?) {
        Span.current()
            .addEvent("arbeidsoppfølgingsperiode_avsluttet", Attributes.of(stringKey("dialogId"), dialogId.toString()))
        logger.warn("Arbeidsoppfølgingsperiode for periodeId=$periodeId, dialogId=$dialogId er avsluttet. Klarte ikke å sende dialogmelding.")
    }
}

@JvmRecord
private data class TempData(
    val periodeId: UUID,
    val dialogId: Long?,
    val response: DialogResultat,
)
