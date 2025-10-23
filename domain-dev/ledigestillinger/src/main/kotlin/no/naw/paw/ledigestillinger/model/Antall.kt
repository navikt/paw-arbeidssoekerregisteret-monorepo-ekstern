package no.naw.paw.ledigestillinger.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Antall(

    @get:JsonProperty("type")
    val type: AntallType,

    @get:JsonProperty("verdi")
    val verdi: kotlin.String? = null,

    @get:JsonProperty("antall")
    val antall: kotlin.Int? = null

)