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
    JsonSubTypes.Type(value = IdentitetsnummerQueryRequest::class, name = "IDENTITETSNUMMER"),
    JsonSubTypes.Type(value = PerioderQueryRequest::class, name = "PERIODER")
)

sealed interface QueryRequest {

    @get:JsonProperty("type")
    val type: QueryType
}
