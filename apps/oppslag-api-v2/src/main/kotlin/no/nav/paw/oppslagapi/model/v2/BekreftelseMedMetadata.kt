package no.nav.paw.oppslagapi.model.v2

import com.fasterxml.jackson.annotation.JsonProperty

data class BekreftelseMedMetadata(

    @get:JsonProperty("status")
    val status: BekreftelseStatus? = null,

    @get:JsonProperty("bekreftelse")
    val bekreftelse: Bekreftelse? = null

)