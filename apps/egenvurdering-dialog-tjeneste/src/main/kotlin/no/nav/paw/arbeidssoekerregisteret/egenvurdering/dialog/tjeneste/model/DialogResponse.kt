package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.http.HttpStatusCode

sealed interface DialogResultat
data class DialogResponse(@field:JsonProperty("id") val dialogId: String) : DialogResultat
data class ArbeidsoppfølgingsperiodeAvsluttet(val httpStatusCode: HttpStatusCode, val errorMessage: String) : DialogResultat
data class BrukerKanIkkeVarsles(val httpStatusCode: HttpStatusCode, val errorMessage: String) : DialogResultat
data class Annen409Feil(val httpStatusCode: HttpStatusCode, val errorMessage: String) : DialogResultat
