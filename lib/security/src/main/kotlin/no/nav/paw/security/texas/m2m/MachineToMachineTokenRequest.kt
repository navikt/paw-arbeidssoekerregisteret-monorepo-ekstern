package no.nav.paw.security.texas.m2m

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.paw.security.texas.IdentityProvider.AZURE_AD

data class MachineToMachineTokenRequest(
    @field:JsonProperty("identity_provider")
    val identityProvider: String = AZURE_AD.value,
    val target: String,
)
