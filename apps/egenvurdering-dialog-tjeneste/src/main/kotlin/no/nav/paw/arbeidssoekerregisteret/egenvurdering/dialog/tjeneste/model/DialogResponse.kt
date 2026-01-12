package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.model

import io.ktor.http.HttpStatusCode

sealed interface DialogResultat
data class DialogResponse(val dialogId: String, val httpStatusCode: HttpStatusCode) : DialogResultat
data class ArbeidsoppfølgingsperiodeAvsluttet(val httpStatusCode: HttpStatusCode, val errorMessage: String) : DialogResultat
data class BrukerKanIkkeVarsles(val httpStatusCode: HttpStatusCode, val errorMessage: String) : DialogResultat
data class Annen409Feil(val httpStatusCode: HttpStatusCode, val errorMessage: String) : DialogResultat