package no.naw.paw.ledigestillinger.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Klassifisering(

    @get:JsonProperty("type")
    val type: KlassifiseringType,

    @get:JsonProperty("kode")
    val kode: String,

    @get:JsonProperty("navn")
    val navn: String

)