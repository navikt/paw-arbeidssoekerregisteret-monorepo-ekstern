package no.nav.paw.oppslagapi.model.v3

import com.fasterxml.jackson.annotation.JsonProperty

data class Bruker(

    @field:JsonProperty("type")
    val type: BrukerType = BrukerType.UKJENT_VERDI,

    @field:JsonProperty("id")
    val id: String,

    @field:JsonProperty("sikkerhetsnivaa")
    val sikkerhetsnivaa: String? = null
)