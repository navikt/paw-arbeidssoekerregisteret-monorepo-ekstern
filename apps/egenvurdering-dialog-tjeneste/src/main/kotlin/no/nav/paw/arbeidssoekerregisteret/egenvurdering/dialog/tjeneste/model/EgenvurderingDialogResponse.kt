package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.model

import com.fasterxml.jackson.annotation.JsonProperty

data class EgenvurderingDialogResponse(
    @field:JsonProperty("dialogId")
    val dialogId: Long?
)
