package no.nav.paw.oppslagapi.model.v3

import com.fasterxml.jackson.annotation.JsonProperty

enum class HendelseType(val value: String) {

    @JsonProperty(value = "PERIODE_STARTET_V1")
    PERIODE_STARTET_V1("PERIODE_STARTET_V1"),

    @JsonProperty(value = "PERIODE_AVSLUTTET_V1")
    PERIODE_AVSLUTTET_V1("PERIODE_AVSLUTTET_V1"),

    @JsonProperty(value = "OPPLYSNINGER_V4")
    OPPLYSNINGER_V4("OPPLYSNINGER_V4"),

    @JsonProperty(value = "PROFILERING_V1")
    PROFILERING_V1("PROFILERING_V1"),

    @JsonProperty(value = "EGENVURDERING_V1")
    EGENVURDERING_V1("EGENVURDERING_V1"),

    @JsonProperty(value = "BEKREFTELSE_V1")
    BEKREFTELSE_V1("BEKREFTELSE_V1"),

    @JsonProperty(value = "PAA_VEGNE_AV_START_V1")
    PAA_VEGNE_AV_START_V1("PAA_VEGNE_AV_START_V1"),

    @JsonProperty(value = "PAA_VEGNE_AV_STOPP_V1")
    PAA_VEGNE_AV_STOPP_V1("PAA_VEGNE_AV_STOPP_V1");

    override fun toString(): String = value
}

