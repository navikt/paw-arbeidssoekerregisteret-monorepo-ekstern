package no.nav.paw.arbeidssoekerregisteret.model

import com.fasterxml.jackson.annotation.JsonProperty

enum class EgenvurdertTil(val value: String) {

    @JsonProperty(value = "ANTATT_BEHOV_FOR_VEILEDNING")
    ANTATT_BEHOV_FOR_VEILEDNING("ANTATT_BEHOV_FOR_VEILEDNING"),

    @JsonProperty(value = "ANTATT_GODE_MULIGHETER")
    ANTATT_GODE_MULIGHETER("ANTATT_GODE_MULIGHETER"),

    @JsonProperty(value = "OPPGITT_HINDRINGER")
    OPPGITT_HINDRINGER("OPPGITT_HINDRINGER");

    override fun toString(): String = value
}