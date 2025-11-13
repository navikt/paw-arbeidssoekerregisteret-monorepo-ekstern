package no.nav.paw.oppslagapi.model.v3

import com.fasterxml.jackson.annotation.JsonProperty

enum class ProfilertTil(val value: String) {

    @JsonProperty(value = "UKJENT_VERDI")
    UKJENT_VERDI("UKJENT_VERDI"),

    @JsonProperty(value = "UDEFINERT")
    UDEFINERT("UDEFINERT"),

    @JsonProperty(value = "ANTATT_GODE_MULIGHETER")
    ANTATT_GODE_MULIGHETER("ANTATT_GODE_MULIGHETER"),

    @JsonProperty(value = "ANTATT_BEHOV_FOR_VEILEDNING")
    ANTATT_BEHOV_FOR_VEILEDNING("ANTATT_BEHOV_FOR_VEILEDNING"),

    @JsonProperty(value = "OPPGITT_HINDRINGER")
    OPPGITT_HINDRINGER("OPPGITT_HINDRINGER");

    override fun toString(): String = value
}

