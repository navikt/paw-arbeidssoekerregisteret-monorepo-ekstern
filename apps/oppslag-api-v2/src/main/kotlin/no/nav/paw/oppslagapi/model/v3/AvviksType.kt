package no.nav.paw.oppslagapi.model.v3

import com.fasterxml.jackson.annotation.JsonProperty

enum class AvviksType(val value: String) {

    @JsonProperty(value = "UKJENT_VERDI")
    UKJENT_VERDI("UKJENT_VERDI"),

    @JsonProperty(value = "FORSINKELSE")
    FORSINKELSE("FORSINKELSE"),

    @JsonProperty(value = "RETTING")
    RETTING("RETTING"),

    @JsonProperty(value = "SLETTET")
    SLETTET("SLETTET"),

    @JsonProperty(value = "TIDSPUNKT_KORRIGERT")
    TIDSPUNKT_KORRIGERT("TIDSPUNKT_KORRIGERT");

    override fun toString(): String = value
}

