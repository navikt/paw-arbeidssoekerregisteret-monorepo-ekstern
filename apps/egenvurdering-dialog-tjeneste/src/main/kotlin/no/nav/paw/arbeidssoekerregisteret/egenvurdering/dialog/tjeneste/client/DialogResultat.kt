package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.client

import com.fasterxml.jackson.annotation.JsonProperty

sealed interface DialogResultat
data class DialogResponse(@field:JsonProperty("id") val dialogId: String) : DialogResultat
object ArbeidsoppfølgingsperiodeAvsluttet : DialogResultat
