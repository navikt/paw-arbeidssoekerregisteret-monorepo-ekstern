package no.naw.paw.ledigestillinger.model

import com.fasterxml.jackson.annotation.JsonProperty

data class StyrkKode(

    @get:JsonProperty("kode")
    val kode: String,
    @get:JsonProperty("navn")
    val navn: String
)