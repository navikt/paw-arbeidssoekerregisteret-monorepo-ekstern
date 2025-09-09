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
                            traceNyMelding(response.dialogId, periodeId)
                        } else if (eksisterendeDialogId == response.dialogId.toLong()) {
                            traceNyMeldingPåEksisterendeTråd(response.dialogId, periodeId)
                        } else if (eksisterendeDialogId != response.dialogId.toLong()) {
                            traceEndringAvDialogId(eksisterendeDialogId, response, periodeId)
                            //periodeIdDialogIdRepository.update(egenvurdering.periodeId, response.dialogId.toLong())
                        }
                    }

                    is ArbeidsoppfølgingsperiodeAvsluttet -> {
                        traceArbeidsoppfølgingsperiodeAvsluttet(periodeId, eksisterendeDialogId)
                    }
                }
            }
    }
}

private fun traceEndringAvDialogId(dialogId: Long, response: DialogResponse, periodeId: UUID) {
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
        .addEvent("arbeidsoppfoelgingsperiode_avsluttet", Attributes.of(stringKey("dialogId"), dialogId.toString()))
    logger.warn("Arbeidsoppfølgingsperiode for periodeId=$periodeId, dialogId=$dialogId er avsluttet. Klarte ikke å sende dialogmelding.")
}

private fun traceNyMeldingPåEksisterendeTråd(dialogId: String, periodeId: UUID) {
    Span.current()
        .addEvent("ny_melding_på_eksisterende_traad", Attributes.of(stringKey("dialogId"), dialogId))
    logger.info("Ny melding på eksisterende tråd med dialogId=$dialogId for periodeId=$periodeId")
}

private fun traceNyMelding(dialogId: String, periodeId: UUID) {
    Span.current().addEvent("ny_melding", Attributes.of(stringKey("dialogId"), dialogId))
    logger.info("Ny meldingmed dialogId=$dialogId for periodeId=$periodeId")
}

@JvmRecord
private data class TempData(
    val periodeId: UUID,
    val dialogId: Long?,
    val response: DialogResultat,
)
