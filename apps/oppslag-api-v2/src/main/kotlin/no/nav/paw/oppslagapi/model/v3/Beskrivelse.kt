package no.nav.paw.oppslagapi.model.v3

import com.fasterxml.jackson.annotation.JsonProperty

enum class Beskrivelse(val value: String) {

    @JsonProperty(value = "UKJENT_VERDI")
    UKJENT_VERDI("UKJENT_VERDI"),

    @JsonProperty(value = "UDEFINERT")
    UDEFINERT("UDEFINERT"),

    @JsonProperty(value = "HAR_SAGT_OPP")
    HAR_SAGT_OPP("HAR_SAGT_OPP"),

    @JsonProperty(value = "HAR_BLITT_SAGT_OPP")
    HAR_BLITT_SAGT_OPP("HAR_BLITT_SAGT_OPP"),

    @JsonProperty(value = "ER_PERMITTERT")
    ER_PERMITTERT("ER_PERMITTERT"),

    @JsonProperty(value = "ALDRI_HATT_JOBB")
    ALDRI_HATT_JOBB("ALDRI_HATT_JOBB"),

    @JsonProperty(value = "IKKE_VAERT_I_JOBB_SISTE_2_AAR")
    IKKE_VAERT_I_JOBB_SISTE_2_AAR("IKKE_VAERT_I_JOBB_SISTE_2_AAR"),

    @JsonProperty(value = "AKKURAT_FULLFORT_UTDANNING")
    AKKURAT_FULLFORT_UTDANNING("AKKURAT_FULLFORT_UTDANNING"),

    @JsonProperty(value = "VIL_BYTTE_JOBB")
    VIL_BYTTE_JOBB("VIL_BYTTE_JOBB"),

    @JsonProperty(value = "USIKKER_JOBBSITUASJON")
    USIKKER_JOBBSITUASJON("USIKKER_JOBBSITUASJON"),

    @JsonProperty(value = "MIDLERTIDIG_JOBB")
    MIDLERTIDIG_JOBB("MIDLERTIDIG_JOBB"),

    @JsonProperty(value = "DELTIDSJOBB_VIL_MER")
    DELTIDSJOBB_VIL_MER("DELTIDSJOBB_VIL_MER"),

    @JsonProperty(value = "NY_JOBB")
    NY_JOBB("NY_JOBB"),

    @JsonProperty(value = "KONKURS")
    KONKURS("KONKURS"),

    @JsonProperty(value = "ANNET")
    ANNET("ANNET");

    override fun toString(): String = value
}

