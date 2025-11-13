package no.nav.paw.oppslagapi.model.v3

import com.fasterxml.jackson.annotation.JsonProperty

enum class BrukerType(val value: String) {

    @JsonProperty(value = "SLUTTBRUKER")
    SLUTTBRUKER("SLUTTBRUKER"),

    @JsonProperty(value = "VEILEDER")
    VEILEDER("VEILEDER"),

    @JsonProperty(value = "SYSTEM")
    SYSTEM("SYSTEM"),

    @JsonProperty(value = "UDEFINERT")
    UDEFINERT("UDEFINERT"),

    @JsonProperty(value = "UKJENT_VERDI")
    UKJENT_VERDI("UKJENT_VERDI");

    override fun toString(): String = value
}

