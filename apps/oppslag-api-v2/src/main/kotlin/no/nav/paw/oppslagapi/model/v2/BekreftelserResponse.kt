package no.nav.paw.oppslagapi.model.v2

import com.fasterxml.jackson.annotation.JsonProperty

data class BekreftelserResponse(

    @get:JsonProperty("bekreftelser")
    val bekreftelser: List<BekreftelseMedMetadata>
)