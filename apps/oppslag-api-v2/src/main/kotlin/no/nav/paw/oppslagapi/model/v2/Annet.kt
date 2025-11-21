package no.nav.paw.oppslagapi.model.v2

import com.fasterxml.jackson.annotation.JsonProperty

data class Annet(

    @get:JsonProperty("andreForholdHindrerArbeid")
    val andreForholdHindrerArbeid: JaNeiVetIkke? = JaNeiVetIkke.UKJENT_VERDI
)