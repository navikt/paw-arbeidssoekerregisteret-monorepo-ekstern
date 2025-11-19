package no.nav.paw.oppslagapi.model.v3

import com.fasterxml.jackson.annotation.JsonProperty

data class Utdanning(

    @field:JsonProperty("nus")
    val nus: String,

    @field:JsonProperty("bestaatt")
    val bestaatt: JaNeiVetIkke? = JaNeiVetIkke.UKJENT_VERDI,

    @field:JsonProperty("godkjent")
    val godkjent: JaNeiVetIkke? = JaNeiVetIkke.UKJENT_VERDI
)