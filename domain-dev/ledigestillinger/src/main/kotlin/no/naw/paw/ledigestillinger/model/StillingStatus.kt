package no.naw.paw.ledigestillinger.model

import com.fasterxml.jackson.annotation.JsonProperty

enum class StillingStatus(val value: String) {

    @JsonProperty(value = "AKTIV")
    AKTIV("AKTIV"),

    @JsonProperty(value = "INAKTIV")
    INAKTIV("INAKTIV"),

    @JsonProperty(value = "STOPPET")
    STOPPET("STOPPET"),

    @JsonProperty(value = "SLETTET")
    SLETTET("SLETTET"),

    @JsonProperty(value = "AVVIST")
    AVVIST("AVVIST");

    override fun toString(): String = value

    companion object {
        fun encode(data: Any?): String? = if (data is StillingStatus) "$data" else null

        fun decode(data: Any?): StillingStatus? = data?.let {
            val normalizedData = "$it".lowercase()
            values().firstOrNull { value ->
                it == value || normalizedData == "$value".lowercase()
            }
        }
    }
}

