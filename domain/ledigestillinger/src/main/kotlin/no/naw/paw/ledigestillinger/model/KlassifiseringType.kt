package no.naw.paw.ledigestillinger.model

import com.fasterxml.jackson.annotation.JsonProperty

enum class KlassifiseringType(val value: String) {

    @JsonProperty(value = "ESCO")
    ESCO("ESCO"),

    @JsonProperty(value = "JANZZ")
    JANZZ("JANZZ"),

    @JsonProperty(value = "STYRK08")
    STYRK08("STYRK08"),

    @JsonProperty(value = "STYRK08NAV")
    STYRK08NAV("STYRK08NAV");
}
