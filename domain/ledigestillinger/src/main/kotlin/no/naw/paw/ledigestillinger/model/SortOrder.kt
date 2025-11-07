package no.naw.paw.ledigestillinger.model

import com.fasterxml.jackson.annotation.JsonProperty

enum class SortOrder(val value: String) {

    @JsonProperty(value = "ASC")
    ASC("ASC"),

    @JsonProperty(value = "DESC")
    DESC("DESC");
}