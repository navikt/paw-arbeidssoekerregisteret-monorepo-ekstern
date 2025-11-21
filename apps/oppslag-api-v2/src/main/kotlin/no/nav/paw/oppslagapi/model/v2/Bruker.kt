package no.nav.paw.oppslagapi.model.v2

import com.fasterxml.jackson.annotation.JsonProperty

data class Bruker(

    @get:JsonProperty("type")
    val type: BrukerType,

    @get:JsonProperty("id")
    val id: String,

    @get:JsonProperty("sikkerhetsnivaa")
    val sikkerhetsnivaa: String? = null
)