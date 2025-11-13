package no.nav.paw.oppslagapi.model.v3

import com.fasterxml.jackson.annotation.JsonProperty

enum class JaNeiVetIkke(val value: String) {

    @JsonProperty(value = "UKJENT_VERDI")
    UKJENT_VERDI("UKJENT_VERDI"),

    @JsonProperty(value = "JA")
    JA("JA"),

    @JsonProperty(value = "NEI")
    NEI("NEI"),

    @JsonProperty(value = "VET_IKKE")
    VET_IKKE("VET_IKKE");

    override fun toString(): String = value

    companion object {
        fun encode(data: Any?): String? = if (data is JaNeiVetIkke) "$data" else null

        fun decode(data: Any?): JaNeiVetIkke? = data?.let {
            val normalizedData = "$it".lowercase()
            entries.firstOrNull { value ->
                it == value || normalizedData == "$value".lowercase()
            }
        }
    }
}

