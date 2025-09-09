package no.nav.paw.security.texas.m2m

import com.fasterxml.jackson.annotation.JsonProperty

data class MachineToMachineTokenResponse(
    @field:JsonProperty("access_token")
    val accessToken: String,
)