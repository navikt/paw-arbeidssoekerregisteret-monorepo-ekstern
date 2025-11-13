package no.nav.paw.oppslagapi.model.v3

import com.fasterxml.jackson.annotation.JsonProperty

enum class BekreftelsStatus(val value: String) {

    @JsonProperty(value = "GYLDIG")
    GYLDIG("GYLDIG"),

    @JsonProperty(value = "UVENTET_KILDE")
    UVENTET_KILDE("UVENTET_KILDE"),

    @JsonProperty(value = "UTENFOR_PERIODE")
    UTENFOR_PERIODE("UTENFOR_PERIODE");

    override fun toString(): String = value
}

