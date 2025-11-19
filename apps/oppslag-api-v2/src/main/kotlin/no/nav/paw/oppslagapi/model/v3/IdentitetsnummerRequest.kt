package no.nav.paw.oppslagapi.model.v3

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName

@JsonTypeName("IDENTITETSNUMMER")
data class IdentitetsnummerRequest(

    @field:JsonProperty("type")
    override val type: QueryType = QueryType.IDENTITETSNUMMER,

    @field:JsonProperty("identitetsnummer")
    val identitetsnummer: String
) : SingleQueryRequest, ListQueryRequest