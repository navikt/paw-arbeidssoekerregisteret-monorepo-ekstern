package no.naw.paw.ledigestillinger.model

import com.fasterxml.jackson.annotation.JsonProperty

data class FinnStillingerResponse(

    @get:JsonProperty("stillinger")
    val stillinger: List<Stilling>? = null
)