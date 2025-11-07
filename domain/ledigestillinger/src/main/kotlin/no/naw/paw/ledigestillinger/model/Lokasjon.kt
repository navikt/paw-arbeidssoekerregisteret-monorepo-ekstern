package no.naw.paw.ledigestillinger.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Lokasjon(

    @get:JsonProperty("land")
    val land: String,
    @get:JsonProperty("postkode")
    val postkode: String? = null,
    @get:JsonProperty("poststed")
    val poststed: String? = null,
    @get:JsonProperty("kommune")
    val kommune: String? = null,
    @get:JsonProperty("kommunenummer")
    val kommunenummer: String? = null,
    @get:JsonProperty("fylke")
    val fylke: String? = null,
    @get:JsonProperty("fylkesnummer")
    val fylkesnummer: String? = null
)