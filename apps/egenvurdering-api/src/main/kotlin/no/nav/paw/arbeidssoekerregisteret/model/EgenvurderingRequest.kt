package no.nav.paw.arbeidssoekerregisteret.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

data class EgenvurderingRequest(

    @get:JsonProperty("profileringId")
    val profileringId: UUID,

    @get:JsonProperty("egenvurdering")
    val egenvurdering: EgenvurdertTil
)