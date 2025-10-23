package no.naw.paw.ledigestillinger.model

import com.fasterxml.jackson.annotation.JsonProperty

enum class FristType(val value: kotlin.String) {

    @JsonProperty(value = "UKJENT")
    UKJENT("UKJENT"),

    @JsonProperty(value = "SNAREST")
    SNAREST("SNAREST"),

    @JsonProperty(value = "DATO")
    DATO("DATO");
}

