package no.nav.paw.oppslagapi.model.v2

import com.fasterxml.jackson.annotation.JsonProperty

enum class BrukerType(val value: String) {
    @JsonProperty(value = "UKJENT_VERDI")
    UKJENT_VERDI("UKJENT_VERDI"),

    @JsonProperty(value = "UDEFINERT")
    UDEFINERT("UDEFINERT"),

    @JsonProperty(value = "VEILEDER")
    VEILEDER("VEILEDER"),

    @JsonProperty(value = "SYSTEM")
    SYSTEM("SYSTEM"),

    @JsonProperty(value = "SLUTTBRUKER")
    SLUTTBRUKER("SLUTTBRUKER");
}