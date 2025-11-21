package no.nav.paw.oppslagapi.model.v2


import com.fasterxml.jackson.annotation.JsonProperty

enum class Bekreftelsesloesning(val value: String) {

    @JsonProperty(value = "UKJENT_VERDI")
    UKJENT_VERDI("UKJENT_VERDI"),

    @JsonProperty(value = "ARBEIDSSOEKERREGISTERET")
    ARBEIDSSOEKERREGISTERET("ARBEIDSSOEKERREGISTERET"),

    @JsonProperty(value = "DAGPENGER")
    DAGPENGER("DAGPENGER"),

    @JsonProperty(value = "FRISKMELDT_TIL_ARBEIDSFORMIDLING")
    FRISKMELDT_TIL_ARBEIDSFORMIDLING("FRISKMELDT_TIL_ARBEIDSFORMIDLING");

    override fun toString(): String = value
}

