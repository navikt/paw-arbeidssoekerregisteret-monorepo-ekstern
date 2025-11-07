package no.naw.paw.ledigestillinger.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Kommune(

    @get:JsonProperty("kommunenummer")
    val kommunenummer: String
)