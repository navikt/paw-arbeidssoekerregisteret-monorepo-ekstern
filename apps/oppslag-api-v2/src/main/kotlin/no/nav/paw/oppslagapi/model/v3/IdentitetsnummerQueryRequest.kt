package no.nav.paw.oppslagapi.model.v3

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.paw.felles.model.Identitetsnummer

@JsonTypeName("IDENTITETSNUMMER")
data class IdentitetsnummerQueryRequest(

    @field:JsonProperty("identitetsnummer")
    val identitetsnummer: Identitetsnummer
) : QueryRequest {

    @field:JsonProperty("type")
    override val type: QueryType = QueryType.IDENTITETSNUMMER
}