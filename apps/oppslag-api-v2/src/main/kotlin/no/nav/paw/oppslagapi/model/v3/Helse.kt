package no.nav.paw.oppslagapi.model.v3

import com.fasterxml.jackson.annotation.JsonProperty

data class Helse(

    @field:JsonProperty("helsetilstandHindrerArbeid")
    val helsetilstandHindrerArbeid: JaNeiVetIkke? = JaNeiVetIkke.UKJENT_VERDI
)