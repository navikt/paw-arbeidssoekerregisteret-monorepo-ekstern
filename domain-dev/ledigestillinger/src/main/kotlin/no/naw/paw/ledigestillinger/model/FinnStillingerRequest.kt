package no.naw.paw.ledigestillinger.model

import com.fasterxml.jackson.annotation.JsonProperty

data class FinnStillingerRequest(

    @get:JsonProperty("soekeord")
    val soekeord: kotlin.collections.List<kotlin.String>? = null,

    @get:JsonProperty("kategorier")
    val kategorier: kotlin.collections.List<Kategori>,

    @get:JsonProperty("fylker")
    val fylker: kotlin.collections.List<Fylke>,

    @get:JsonProperty("page")
    val page: kotlin.Int = 1,

    @get:JsonProperty("pageSize")
    val pageSize: kotlin.Int = 10
)