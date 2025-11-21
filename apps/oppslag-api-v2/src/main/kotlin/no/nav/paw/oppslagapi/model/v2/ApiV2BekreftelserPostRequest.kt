package no.nav.paw.oppslagapi.model.v2

import com.fasterxml.jackson.annotation.JsonProperty

data class ApiV2BekreftelserPostRequest(

    @get:JsonProperty("identitetsnummer")
    val identitetsnummer: String,

    @get:JsonProperty("perioder")
    val perioder: List<java.util.UUID>
)