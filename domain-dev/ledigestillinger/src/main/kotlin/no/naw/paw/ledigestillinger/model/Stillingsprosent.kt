package no.naw.paw.ledigestillinger.model

import com.fasterxml.jackson.annotation.JsonProperty

enum class Stillingsprosent(val value: String) {

    @JsonProperty(value = "HELTID")
    HELTID("HELTID"),

    @JsonProperty(value = "DELTID")
    DELTID("DELTID"),

    @JsonProperty(value = "UKJENT")
    UKJENT("UKJENT");
}

