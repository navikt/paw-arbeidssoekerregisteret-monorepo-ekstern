package no.nav.paw.oppslagapi.model.v2

import com.fasterxml.jackson.annotation.JsonProperty

data class Utdanning(

    @get:JsonProperty("nus")
    val nus: String,

    @get:JsonProperty("bestaatt")
    val bestaatt: JaNeiVetIkke? = JaNeiVetIkke.UKJENT_VERDI,

    @get:JsonProperty("godkjent")
    val godkjent: JaNeiVetIkke? = JaNeiVetIkke.UKJENT_VERDI
)