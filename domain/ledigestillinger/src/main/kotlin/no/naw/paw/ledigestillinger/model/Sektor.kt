package no.naw.paw.ledigestillinger.model

import com.fasterxml.jackson.annotation.JsonProperty

enum class Sektor(val value: String) {

    @JsonProperty(value = "OFFENTLIG")
    OFFENTLIG("OFFENTLIG"),

    @JsonProperty(value = "PRIVAT")
    PRIVAT("PRIVAT"),

    @JsonProperty(value = "UKJENT")
    UKJENT("UKJENT");
}

