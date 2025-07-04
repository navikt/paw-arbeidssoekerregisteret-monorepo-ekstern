package no.nav.paw.arbeidssoekerregisteret

import no.nav.paw.arbeidssoekerregisteret.utils.buildApplicationLogger
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

class ANTATT_BEHOV_FOR_VEILEDNING_OG_VIL_HA_HJELP(dato: String) : Dialogmelding(
    overskrift = "Egenvurdering $dato",
    tekst = "Nav sin vurdering: Vi tror du vil trenge hjelp fra en veileder for å nå ditt mål om arbeid.\n\nMin vurdering: Ja, jeg ønsker hjelp. \n\nDette er en automatisk generert melding",
    venterPaaSvarFraNav = true
)

class ANTATT_BEHOV_FOR_VEILEDNING_MEN_VIL_KLARE_SEG_SELV(dato: String) : Dialogmelding(
    overskrift = "Egenvurdering $dato",
    tekst = "Nav sin vurdering: Vi tror du vil trenge hjelp fra en veileder for å nå ditt mål om arbeid.\n\nMin vurdering: Nei, jeg vil gjerne klare meg selv. \n\nDette er en automatisk generert melding",
    venterPaaSvarFraNav = false
)

class DialogService(val veilarbdialogClient: VeilarbdialogClient) {
    fun handleRecords(records: ConsumerRecords<Long, Egenvurdering>) {
        logger.info("Received ${records.count()} records from Kafka")
        records.forEach { record ->
            val egenvurdering = record.value()
            val dialogmelding = egenvurdering.toDialogmelding()
            //val response = runBlocking { veilarbdialogClient.lagEllerOppdaterDialog(dialogmelding.toDialogRequest()) }
            TODO("handle response")
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
            ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING -> ANTATT_BEHOV_FOR_VEILEDNING_OG_VIL_HA_HJELP(dato)
            else -> throw UnsupportedOperationException("Har ikke støtte for denne kombinasjonen av profilertTil og egenvurdering: ${this.profilertTil} og ${this.egenvurdering}")
        }

        else -> throw UnsupportedOperationException("Har ikke støtte for denne profilertTil: ${this.profilertTil}")
    }
}

fun Dialogmelding.toDialogRequest(egenvurdering: Egenvurdering): DialogRequest {
    return DialogRequest(
        tekst = this.tekst,
        dialogId = null,
        overskrift = this.overskrift,
        aktivitetId = "Egenvurdering", // TODO: sjekk og bytt ut
        venterPaaSvarFraNav = this.venterPaaSvarFraNav,
        venterPaaSvarFraBruker = false, // Assuming this is always false for these messages
        egenskaper = listOf("egenvurdering"), // TODO: sjekk og bytt ut
        fnr = egenvurdering.sendtInnAv.utfoertAv.id
    )
}
