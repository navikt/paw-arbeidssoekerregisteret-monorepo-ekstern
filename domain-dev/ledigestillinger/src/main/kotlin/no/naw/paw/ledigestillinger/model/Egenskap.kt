package no.naw.paw.ledigestillinger.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Egenskap(

    @get:JsonProperty("key")
    val key: String,

    @get:JsonProperty("value")
    val `value`: String
)