package no.nav.paw.oppslagapi.model.v3

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName

@JsonTypeName("IDENTITETSNUMMER")
data class IdentitetsnummerRequest(

    @get:JsonProperty("type")
    override val type: QueryType,

    @get:JsonProperty("identitetsnummer")
    val identitetsnummer: String
) : SingleQueryRequest, ListQueryRequest