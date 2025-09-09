package no.nav.paw.security.texas.obo

import com.fasterxml.jackson.annotation.JsonProperty

data class OnBehalfOfResponse(
    @field:JsonProperty("access_token")
    val accessToken: String,
)