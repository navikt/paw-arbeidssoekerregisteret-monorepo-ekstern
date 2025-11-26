package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

data class EgenvurderingDialogRequest(
    @field:JsonProperty("periodeId")
    val periodeId: UUID
)
