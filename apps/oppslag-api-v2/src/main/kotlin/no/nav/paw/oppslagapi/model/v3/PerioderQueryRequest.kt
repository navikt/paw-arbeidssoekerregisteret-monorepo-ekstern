package no.nav.paw.oppslagapi.model.v3

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import java.util.*

@JsonTypeName("PERIODER")
data class PerioderQueryRequest(

    @field:JsonProperty("perioder")
    val perioder: List<UUID>
) : QueryRequest {

    @field:JsonProperty("type")
    override val type: QueryType = QueryType.PERIODER
}