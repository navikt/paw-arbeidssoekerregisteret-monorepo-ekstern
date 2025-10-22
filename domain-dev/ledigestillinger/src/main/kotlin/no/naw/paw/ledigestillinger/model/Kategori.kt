package no.naw.paw.ledigestillinger.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Kategori(

    @get:JsonProperty("kode")
    val kode: String,
    @get:JsonProperty("normalisertKode")
    val normalisertKode: String,
    @get:JsonProperty("navn")
    val navn: String
)