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
}

