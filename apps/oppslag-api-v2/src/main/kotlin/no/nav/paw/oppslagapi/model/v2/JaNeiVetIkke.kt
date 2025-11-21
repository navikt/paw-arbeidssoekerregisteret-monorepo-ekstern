package no.nav.paw.oppslagapi.model.v2

import com.fasterxml.jackson.annotation.JsonProperty

enum class JaNeiVetIkke(val value: String) {

    @JsonProperty(value = "UKJENT_VERDI")
    UKJENT_VERDI("UKJENT_VERDI"),

    @JsonProperty(value = "JA")
    JA("JA"),

    @JsonProperty(value = "NEI")
    NEI("NEI"),

    @JsonProperty(value = "VET_IKKE")
    VET_IKKE("VET_IKKE");

    override fun toString(): String = value
}

