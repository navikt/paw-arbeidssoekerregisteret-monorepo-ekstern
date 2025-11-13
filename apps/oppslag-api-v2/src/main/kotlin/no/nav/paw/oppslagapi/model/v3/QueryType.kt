package no.nav.paw.oppslagapi.model.v3

import com.fasterxml.jackson.annotation.JsonProperty

enum class QueryType(val value: String) {

    @JsonProperty(value = "PERIODE")
    PERIODE("PERIODE"),

    @JsonProperty(value = "PERIODE_LISTE")
    PERIODE_LISTE("PERIODE_LISTE"),

    @JsonProperty(value = "IDENTITETSNUMMER")
    IDENTITETSNUMMER("IDENTITETSNUMMER");

    override fun toString(): String = value
}

