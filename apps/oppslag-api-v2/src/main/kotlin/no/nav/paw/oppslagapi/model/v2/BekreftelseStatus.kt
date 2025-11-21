package no.nav.paw.oppslagapi.model.v2

import com.fasterxml.jackson.annotation.JsonProperty

enum class BekreftelseStatus(val value: String) {
    @JsonProperty(value = "GYLDIG")
    GYLDIG("GYLDIG"),

    @JsonProperty(value = "UVENTET_KILDE")
    UVENTET_KILDE("UVENTET_KILDE"),

    @JsonProperty(value = "UTENFOR_PERIODE")
    UTENFOR_PERIODE("UTENFOR_PERIODE");
}