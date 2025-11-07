package no.naw.paw.ledigestillinger.model

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
    JsonSubTypes.Type(value = FinnStillingerByEgenskaperRequest::class, name = "BY_EGENSKAPER"),
    JsonSubTypes.Type(value = FinnStillingerByUuidListeRequest::class, name = "BY_UUID_LISTE")
)
sealed interface FinnStillingerRequest {

    @get:JsonProperty("type")
    val type: FinnStillingerType
}
