package no.naw.paw.ledigestillinger.model

import com.fasterxml.jackson.annotation.JsonProperty

enum class FinnStillingerType(val value: String) {

    @JsonProperty(value = "BY_EGENSKAPER")
    BY_EGENSKAPER("BY_EGENSKAPER"),

    @JsonProperty(value = "BY_UUID_LISTE")
    BY_UUID_LISTE("BY_UUID_LISTE");
}

