package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.service

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import kotlinx.coroutines.runBlocking
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.model.ArbeidsoppfølgingsperiodeAvsluttet
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.model.DialogResponse
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.client.VeilarbdialogClient
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.client.VeilarbdialogClientException
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.model.toDialogRequest
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.model.ProfileringIkkeStøttet
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.model.ProfileringKombinasjonIkkeStøttet
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.model.tilDialogmelding
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.repository.PeriodeIdDialogIdRepository
import no.nav.paw.arbeidssokerregisteret.api.v3.Egenvurdering
import no.nav.paw.logging.logger.buildLogger
import org.apache.kafka.clients.consumer.ConsumerRecords
import java.util.*

class DialogService(
    private val applicationConfig: ApplicationConfig,
    private val veilarbdialogClient: VeilarbdialogClient,
    private val periodeIdDialogIdRepository: PeriodeIdDialogIdRepository
) {
    private val logger = buildLogger

    fun varsleVeilederOmEgenvurderingAvProfilering(records: ConsumerRecords<Long, Egenvurdering>) {
        records.asSequence()
            .mapNotNull { record ->
                val egenvurdering = record.value()
                try {
                    val dialogmelding = egenvurdering.tilDialogmelding()
                    val eksisterendeDialogId: Long? =
                        periodeIdDialogIdRepository.getDialogIdOrNull(egenvurdering.periodeId)
                    val dialogRequest = dialogmelding.toDialogRequest(
                        fnr = egenvurdering.sendtInnAv.utfoertAv.id,
                        dialogId = eksisterendeDialogId
                    )
                    val response = runBlocking {
                        try {
                            veilarbdialogClient.lagEllerOppdaterDialog(dialogRequest)
                        } catch (ex: VeilarbdialogClientException) {
                            throw Exception(
                                "Feil ved kommunikasjon med veilarbdialog ved prosessering av egenvurdering:" +
                                        " topic=${record.topic()}, partition=${record.partition()}," +
                                        " offset=${record.offset()}",
                                ex
                            )
                        }
                    }
                    Triple(egenvurdering.periodeId, eksisterendeDialogId, response)
                } catch (e: ProfileringIkkeStøttet) {
                    logger.error(e.message, e)
                    null
                } catch (e: ProfileringKombinasjonIkkeStøttet) {
                    logger.error(e.message, e)
                    null
                }
            }.forEach { (periodeId, eksisterendeDialogId, response) ->
                when (response) {
                    is DialogResponse -> {
                        if (eksisterendeDialogId == null) {
                            periodeIdDialogIdRepository.insert(periodeId, response.dialogId.toLong())
                            traceNyTraad(response.dialogId, periodeId)
                        } else if (eksisterendeDialogId == response.dialogId.toLong()) {
                            traceNyMeldingPaaEksisterendeTraad(response.dialogId, periodeId)
                        } else if (eksisterendeDialogId != response.dialogId.toLong()) {
                            traceEndringAvDialogId(eksisterendeDialogId, response, periodeId)
                            periodeIdDialogIdRepository.update(periodeId, response.dialogId.toLong())
                        }
                    }

                    is ArbeidsoppfølgingsperiodeAvsluttet -> {
                        traceArbeidsoppfoelgingsperiodeAvsluttet(periodeId, eksisterendeDialogId)
                    }
                }
            }
    }

    fun finnDialogIdForPeriodeId(periodeId: UUID): Long? = periodeIdDialogIdRepository.getDialogIdOrNull(periodeId)

    private fun traceEndringAvDialogId(dialogId: Long, response: DialogResponse, periodeId: UUID) {
        Span.current().addEvent(
            "endret_dialog_id", Attributes.of(
                AttributeKey.stringKey("eksisterende_dialogId"), dialogId.toString(),
                AttributeKey.stringKey("ny_dialogId"), response.dialogId
            )
        )
        logger.warn("Fant ikke dialog med $dialogId. Oppdaterer til ny dialogId=${response.dialogId}, for periodeId=$periodeId.")
    }

    private fun traceArbeidsoppfoelgingsperiodeAvsluttet(periodeId: UUID, dialogId: Long?) {
        Span.current()
            .addEvent(
                "arbeidsoppfoelgingsperiode_avsluttet",
                Attributes.of(AttributeKey.stringKey("dialogId"), dialogId.toString())
            )
        logger.warn("Arbeidsoppfølgingsperiode for periodeId=$periodeId, dialogId=$dialogId er avsluttet. Klarte ikke å sende dialogmelding.")
    }

    private fun traceNyMeldingPaaEksisterendeTraad(dialogId: String, periodeId: UUID) {
        Span.current()
            .addEvent("ny_melding_på_eksisterende_traad", Attributes.of(AttributeKey.stringKey("dialogId"), dialogId))
        logger.info("Ny melding på eksisterende tråd med dialogId=$dialogId for periodeId=$periodeId")
    }

    private fun traceNyTraad(dialogId: String, periodeId: UUID) {
        Span.current().addEvent("ny_traad", Attributes.of(AttributeKey.stringKey("dialogId"), dialogId))
        logger.info("Ny traad med dialogId=$dialogId for periodeId=$periodeId")
    }
}