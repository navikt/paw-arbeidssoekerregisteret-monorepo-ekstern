package no.naw.paw.ledigestillinger.model

import com.fasterxml.jackson.annotation.JsonProperty

enum class AntallType(val value: String) {

    @JsonProperty(value = "ANTALL")
    ANTALL("ANTALL"),

    @JsonProperty(value = "UKJENT")
    UKJENT("UKJENT");
}

