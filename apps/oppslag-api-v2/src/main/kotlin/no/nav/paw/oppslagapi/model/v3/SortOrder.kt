package no.nav.paw.oppslagapi.model.v3

import com.fasterxml.jackson.annotation.JsonProperty

enum class SortOrder(val value: String) {

    @JsonProperty(value = "ASC")
    ASC("ASC"),

    @JsonProperty(value = "DESC")
    DESC("DESC");
}