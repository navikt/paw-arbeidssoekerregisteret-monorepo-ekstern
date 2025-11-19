package no.nav.paw.oppslagapi.model.v3

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import java.util.*

@JsonTypeName("PERIODE_ID_LISTE")
data class PeriodeListRequest(

    @field:JsonProperty("type")
    override val type: QueryType = QueryType.PERIODE_ID_LISTE,

    @field:JsonProperty("perioder")
    val perioder: List<UUID>
) : ListQueryRequest