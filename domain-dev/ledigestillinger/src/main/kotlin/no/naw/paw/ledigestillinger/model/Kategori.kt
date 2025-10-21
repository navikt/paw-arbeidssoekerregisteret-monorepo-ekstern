package no.naw.paw.ledigestillinger.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Kategori(

    @get:JsonProperty("kode")
    val kode: String
)