package no.nav.paw.arbeidssoekerregisteret.model

import com.fasterxml.jackson.annotation.JsonProperty

enum class ProfilertTil(val value: String) {

    @JsonProperty(value = "ANTATT_GODE_MULIGHETER")
    ANTATT_GODE_MULIGHETER("ANTATT_GODE_MULIGHETER"),

    @JsonProperty(value = "ANTATT_BEHOV_FOR_VEILEDNING")
    ANTATT_BEHOV_FOR_VEILEDNING("ANTATT_BEHOV_FOR_VEILEDNING"),

    @JsonProperty(value = "OPPGITT_HINDRINGER")
    OPPGITT_HINDRINGER("OPPGITT_HINDRINGER"),

    @JsonProperty(value = "UKJENT_VERDI")
    UKJENT_VERDI("UKJENT_VERDI"),

    @JsonProperty(value = "UDEFINERT")
    UDEFINERT("UDEFINERT");

    override fun toString(): String = value

}

