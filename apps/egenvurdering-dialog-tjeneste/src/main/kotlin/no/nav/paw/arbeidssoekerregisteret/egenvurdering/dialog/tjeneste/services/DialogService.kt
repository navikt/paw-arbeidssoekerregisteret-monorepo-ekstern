package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.services

import kotlinx.coroutines.runBlocking
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.DialogRequest
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.VeilarbdialogClient
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.repositories.DialogRepository
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.utils.buildApplicationLogger
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import no.nav.paw.arbeidssokerregisteret.api.v2.Egenvurdering
import org.apache.kafka.clients.consumer.ConsumerRecords
import java.time.LocalDate

private val logger = buildApplicationLogger

abstract class Dialogmelding(
    val overskrift: String,
    val tekst: String,
    val venterPaaSvarFraNav: Boolean
)

class ANTATT_GODE_MULIGHETER_MEN_VIL_HA_VEILEDNING(dato: String) : Dialogmelding(
    overskrift = "Egenvurdering $dato",
    tekst = "Nav sin vurdering: Vi tror du har gode muligheter til å komme i jobb uten en veileder eller tiltak fra Nav.\n\nMin vurdering: Jeg trenger en veileder for å komme i arbeid. \n\nDette er en automatisk generert melding",
    venterPaaSvarFraNav = true
)

class ANTATT_GODE_MULIGHETER_OG_VIL_KLARE_SEG_SELV(dato: String) : Dialogmelding(
    overskrift = "Egenvurdering $dato",
    tekst = "Nav sin vurdering: Vi tror du har gode muligheter til å komme i jobb uten en veileder eller tiltak fra Nav.\n\nMin vurdering: Jeg klarer meg uten veileder. \n\nDette er en automatisk generert melding",
    venterPaaSvarFraNav = false
)

class ANTATT_BEHOV_FOR_VEILEDNING_OG_VIL_HA_VEILEDNING(dato: String) : Dialogmelding(
    overskrift = "Egenvurdering $dato",
    tekst = "Nav sin vurdering: Vi tror du vil trenge hjelp fra en veileder for å nå ditt mål om arbeid.\n\nMin vurdering: Ja, jeg ønsker hjelp. \n\nDette er en automatisk generert melding",
    venterPaaSvarFraNav = true
)

class ANTATT_BEHOV_FOR_VEILEDNING_MEN_VIL_KLARE_SEG_SELV(dato: String) : Dialogmelding(
    overskrift = "Egenvurdering $dato",
    tekst = "Nav sin vurdering: Vi tror du vil trenge hjelp fra en veileder for å nå ditt mål om arbeid.\n\nMin vurdering: Nei, jeg vil gjerne klare meg selv. \n\nDette er en automatisk generert melding",
    venterPaaSvarFraNav = false
)

class DialogService(
    val veilarbdialogClient: VeilarbdialogClient,
    val dialogRepository: DialogRepository,
) {
    fun handleRecords(records: ConsumerRecords<Long, Egenvurdering>) {
        logger.info("Received ${records.count()} records from Kafka")
        records.forEach { record ->
            val egenvurdering = record.value()
            val dialogmelding = egenvurdering.toDialogmelding()
            val eksisterendeDialogRow = dialogRepository.findDialogId(egenvurdering.id)
            if (eksisterendeDialogRow != null) {
                runBlocking { veilarbdialogClient.lagEllerOppdaterDialog(dialogmelding.toDialogRequest(egenvurdering, eksisterendeDialogRow.dialogId)) }
            } else {
                logger.info("Ingen eksisterende dialogId funnet for egenvurdering ${egenvurdering.id}, oppretter ny dialog")
                val response = runBlocking { veilarbdialogClient.lagEllerOppdaterDialog(dialogmelding.toDialogRequest(egenvurdering, null)) }
                dialogRepository.insertDialogId(egenvurdering.id, response.dialogId)
            }
        }
    }
}

fun Egenvurdering.toDialogmelding(): Dialogmelding {
    val dato = LocalDate.now().toString()
    return when (this.profilertTil) {
        ProfilertTil.ANTATT_GODE_MULIGHETER -> when (this.egenvurdering) {
            ProfilertTil.ANTATT_GODE_MULIGHETER -> ANTATT_GODE_MULIGHETER_OG_VIL_KLARE_SEG_SELV(dato)
            ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING -> ANTATT_GODE_MULIGHETER_MEN_VIL_HA_VEILEDNING(dato)
            else -> throw UnsupportedOperationException("Har ikke støtte for denne kombinasjonen av profilertTil og egenvurdering: ${this.profilertTil} og ${this.egenvurdering}")
        }

        ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING -> when (this.egenvurdering) {
            ProfilertTil.ANTATT_GODE_MULIGHETER -> ANTATT_BEHOV_FOR_VEILEDNING_MEN_VIL_KLARE_SEG_SELV(dato)
            ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING -> ANTATT_BEHOV_FOR_VEILEDNING_OG_VIL_HA_VEILEDNING(dato)
            else -> throw UnsupportedOperationException("Har ikke støtte for denne kombinasjonen av profilertTil og egenvurdering: ${this.profilertTil} og ${this.egenvurdering}")
        }

        //TODO: støtte oppgitt hindringer?
        else -> throw UnsupportedOperationException("Har ikke støtte for denne profilertTil: ${this.profilertTil}")
    }
}

fun Dialogmelding.toDialogRequest(egenvurdering: Egenvurdering, dialogId: String?) =
    DialogRequest(
        tekst = this.tekst,
        dialogId = dialogId,
        overskrift = this.overskrift,
        venterPaaSvarFraNav = this.venterPaaSvarFraNav,
        venterPaaSvarFraBruker = false,
        fnr = egenvurdering.sendtInnAv.utfoertAv.id
    )
