package no.nav.paw.oppslagapi.model.v3

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true
)
@JsonSubTypes(
    JsonSubTypes.Type(value = IdentitetsnummerRequest::class, name = "IDENTITETSNUMMER"),
    JsonSubTypes.Type(value = PeriodeListRequest::class, name = "PERIODE_LISTE")
)

interface ListQueryRequest {

    @get:JsonProperty("type")
    val type: QueryType
}
