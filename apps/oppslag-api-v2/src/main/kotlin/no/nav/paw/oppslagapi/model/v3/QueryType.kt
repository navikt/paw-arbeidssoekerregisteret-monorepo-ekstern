package no.nav.paw.oppslagapi.model.v3

import com.fasterxml.jackson.annotation.JsonProperty

enum class QueryType(val value: String) {

    @JsonProperty(value = "PERIODER")
    PERIODER("PERIODER"),

    @JsonProperty(value = "IDENTITETSNUMMER")
    IDENTITETSNUMMER("IDENTITETSNUMMER");

    override fun toString(): String = value
}

