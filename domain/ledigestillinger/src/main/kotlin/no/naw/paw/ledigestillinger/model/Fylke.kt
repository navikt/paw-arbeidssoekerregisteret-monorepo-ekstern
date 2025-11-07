package no.naw.paw.ledigestillinger.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Fylke(

    @get:JsonProperty("fylkesnummer")
    val fylkesnummer: String? = null,
    @get:JsonProperty("kommuner")
    val kommuner: List<Kommune>
)
