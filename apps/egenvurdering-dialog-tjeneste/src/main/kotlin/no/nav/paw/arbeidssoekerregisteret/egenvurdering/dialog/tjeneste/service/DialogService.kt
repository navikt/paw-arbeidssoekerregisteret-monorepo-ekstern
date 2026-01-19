package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.service

import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import kotlinx.coroutines.runBlocking
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.client.VeilarbdialogClient
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.client.VeilarbdialogClientException
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.model.Annen409Feil
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.model.ArbeidsoppfølgingsperiodeAvsluttet
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.model.BrukerKanIkkeVarsles
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.model.DialogResponse
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.model.ProfileringIkkeStøttet
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.model.ProfileringKombinasjonIkkeStøttet
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.model.tilDialogmelding
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.model.toDialogRequest
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.repository.PeriodeDialogRow
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.repository.PeriodeIdDialogIdTable
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.repository.PeriodeIdDialogIdTable.getByWithAudit

import no.nav.paw.arbeidssokerregisteret.api.v3.Egenvurdering
import no.nav.paw.logging.logger.buildLogger
import org.apache.kafka.clients.consumer.ConsumerRecords
import java.util.*

class DialogService(
    private val veilarbdialogClient: VeilarbdialogClient,
) {
    private val logger = buildLogger

    fun varsleVeilederOmEgenvurderingAvProfilering(records: ConsumerRecords<Long, Egenvurdering>) {
        records.asSequence()
            .mapNotNull { record ->
                val egenvurdering = record.value()
                try {
                    val dialogmelding = egenvurdering.tilDialogmelding()
                    val eksisterendeDialogId: Long? = PeriodeIdDialogIdTable.getBy(egenvurdering.periodeId)?.dialogId
                    val dialogRequest = dialogmelding.toDialogRequest(
                        fnr = egenvurdering.sendtInnAv.utfoertAv.id,
                        dialogId = eksisterendeDialogId
                    )
                    val response = runBlocking {
                        try {
                            veilarbdialogClient.lagEllerOppdaterDialog(dialogRequest)
                        } catch (ex: VeilarbdialogClientException) {
                            PeriodeIdDialogIdTable.insert(
                                egenvurdering.periodeId,
                                dialogId = eksisterendeDialogId,
                                egenvurderingId = egenvurdering.id,
                                httpStatusCode = ex.status,
                                errorMessage = ex.message
                            )
                            throw Exception(
                                "Feil ved kommunikasjon med veilarbdialog ved prosessering av egenvurdering:" +
                                        " topic=${record.topic()}, partition=${record.partition()}," +
                                        " offset=${record.offset()}",
                                ex
                            )
                        }
                    }
                    Triple(egenvurdering, eksisterendeDialogId, response)
                } catch (e: ProfileringIkkeStøttet) {
                    logger.error(e.message, e)
                    null
                } catch (e: ProfileringKombinasjonIkkeStøttet) {
                    logger.error(e.message, e)
                    null
                }
            }.forEach { (egenvurdering, eksisterendeDialogId, response) ->
                when (response) {
                    is DialogResponse -> {
                        if (eksisterendeDialogId == null) {
                            PeriodeIdDialogIdTable.insert(
                                periodeId = egenvurdering.periodeId,
                                egenvurderingId = egenvurdering.id,
                                dialogId = response.dialogId.toLong(),
                                httpStatusCode = response.httpStatusCode
                            )
                            traceNyTraad(response.dialogId)
                        } else if (eksisterendeDialogId == response.dialogId.toLong()) {
                            PeriodeIdDialogIdTable.insert(
                                periodeId = egenvurdering.periodeId,
                                egenvurderingId = egenvurdering.id,
                                dialogId = null,
                                httpStatusCode = response.httpStatusCode
                            )
                            traceNyMeldingPaaEksisterendeTraad(response.dialogId)
                        } else if (eksisterendeDialogId != response.dialogId.toLong()) {
                            PeriodeIdDialogIdTable.update(egenvurdering.periodeId, response.dialogId.toLong())
                            traceEndringAvDialogId(eksisterendeDialogId, response)
                        }
                    }

                    is ArbeidsoppfølgingsperiodeAvsluttet -> {
                        PeriodeIdDialogIdTable.insert(
                            periodeId = egenvurdering.periodeId,
                            dialogId = null,
                            egenvurderingId = egenvurdering.id,
                            response.httpStatusCode,
                            response.errorMessage,
                        )
                        traceArbeidsoppfoelgingsperiodeAvsluttet(eksisterendeDialogId)
                    }

                    is BrukerKanIkkeVarsles -> {
                        PeriodeIdDialogIdTable.insert(
                            periodeId = egenvurdering.periodeId,
                            dialogId = null,
                            egenvurderingId = egenvurdering.id,
                            response.httpStatusCode,
                            response.errorMessage,
                        )
                        traceBrukerKanIkkeVarsles()
                    }

                    is Annen409Feil -> {
                        PeriodeIdDialogIdTable.insert(
                            periodeId = egenvurdering.periodeId,
                            dialogId = null,
                            egenvurderingId = egenvurdering.id,
                            response.httpStatusCode,
                            response.errorMessage,
                        )
                        traceAnnen409Feil()
                    }

                }
            }
    }

    fun finnDialogInfoForPeriodeId(periodeId: UUID): PeriodeDialogRow? = getByWithAudit(periodeId)

    private fun traceEndringAvDialogId(dialogId: Long, response: DialogResponse) {
        Span.current().addEvent(
            "endret_dialog_id",
            Attributes.of(
                stringKey("eksisterende_dialogId"), dialogId.toString(),
                stringKey("ny_dialogId"), response.dialogId
            )
        )
        logger.warn("Fant ikke dialog med $dialogId. Oppdaterer til ny dialogId=${response.dialogId}")
    }

    private fun traceArbeidsoppfoelgingsperiodeAvsluttet(dialogId: Long?) {
        Span.current().addEvent(
            "veilarbdialog_conflict",
            Attributes.of(
                stringKey("dialogId"), dialogId.toString(),
                stringKey("type"), ArbeidsoppfølgingsperiodeAvsluttet::class.simpleName!!
            )
        )
        logger.warn("Arbeidsoppfølgingsperiode for periode med dialogId=$dialogId er avsluttet. Klarte ikke å sende dialogmelding.")
    }

    private fun traceNyMeldingPaaEksisterendeTraad(dialogId: String) {
        Span.current().addEvent("ny_melding_på_eksisterende_traad", Attributes.of(stringKey("dialogId"), dialogId))
        logger.info("Ny melding på eksisterende tråd med dialogId=$dialogId")
    }

    private fun traceAnnen409Feil() {
        Span.current()
            .addEvent("veilarbdialog_conflict", Attributes.of(stringKey("type"), Annen409Feil::class.simpleName!!))
    }

    private fun traceNyTraad(dialogId: String) {
        Span.current().addEvent("ny_traad", Attributes.of(stringKey("dialogId"), dialogId))
        logger.info("Ny traad med dialogId=$dialogId")
    }

    private fun traceBrukerKanIkkeVarsles() {
        Span.current().addEvent(
            "veilarbdialog_conflict", Attributes.of(stringKey("type"), BrukerKanIkkeVarsles::class.simpleName!!)
        )
    }
}
