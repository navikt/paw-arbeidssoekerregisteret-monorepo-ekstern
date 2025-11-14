package no.nav.paw.oppslagapi.model.v3

import com.fasterxml.jackson.annotation.JsonProperty

enum class QueryType(val value: String) {

    @JsonProperty(value = "PERIODE_ID")
    PERIODE_ID("PERIODE_ID"),

    @JsonProperty(value = "PERIODE_ID_LISTE")
    PERIODE_ID_LISTE("PERIODE_ID_LISTE"),

    @JsonProperty(value = "IDENTITETSNUMMER")
    IDENTITETSNUMMER("IDENTITETSNUMMER");

    override fun toString(): String = value
}

