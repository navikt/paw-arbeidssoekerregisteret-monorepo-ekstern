package no.naw.paw.ledigestillinger.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName

@JsonTypeName("BY_EGENSKAPER")
data class FinnStillingerByEgenskaperRequest(

    @get:JsonProperty("type")
    override val type: FinnStillingerType,

    @get:JsonProperty("soekeord")
    val soekeord: List<String>,

    @get:JsonProperty("styrkkoder")
    val styrkkoder: List<String>,

    @get:JsonProperty("fylker")
    val fylker: List<Fylke>,

    @get:JsonProperty("paging")
    val paging: Paging
) : FinnStillingerRequest