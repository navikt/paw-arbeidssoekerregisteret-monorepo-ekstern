package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.model

import com.fasterxml.jackson.annotation.JsonProperty

sealed interface DialogResultat
data class DialogResponse(@field:JsonProperty("id") val dialogId: String) : DialogResultat
object ArbeidsoppfølgingsperiodeAvsluttet : DialogResultat
object BrukerKanIkkeVarsles : DialogResultat
data class Annen409Feil(val body: String) : DialogResultat
