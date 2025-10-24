package no.naw.paw.ledigestillinger.model

import com.fasterxml.jackson.annotation.JsonProperty

data class PagingResponse(

    @get:JsonProperty("page")
    val page: Int = 1,
    @get:JsonProperty("pageSize")
    val pageSize: Int = 10,
    @get:JsonProperty("hitSize")
    val hitSize: Int = 10,
    @get:JsonProperty("sortOrder")
    val sortOrder: SortOrder = SortOrder.ASC

)
