package no.nav.paw.oppslagapi.model.v2

import com.fasterxml.jackson.annotation.JsonProperty

data class PerioderRequest(

    @get:JsonProperty("perioder")
    val perioder: List<java.util.UUID>
)