package no.naw.paw.ledigestillinger.model

import com.fasterxml.jackson.annotation.JsonProperty

data class FinnStillingerRequest(

    @get:JsonProperty("soekeord")
    val soekeord: List<String>,
    @get:JsonProperty("kategorier")
    val kategorier: List<Kategori>,
    @get:JsonProperty("fylker")
    val fylker: List<Fylke>,
    @get:JsonProperty("paging")
    val paging: Paging
)