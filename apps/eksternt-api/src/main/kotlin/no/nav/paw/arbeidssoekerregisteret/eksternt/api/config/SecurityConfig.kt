package no.nav.paw.arbeidssoekerregisteret.eksternt.api.config

import no.nav.security.token.support.v2.IssuerConfig
import no.nav.security.token.support.v2.RequiredClaims
import no.nav.security.token.support.v2.TokenSupportConfig

const val SECURITY_CONFIG = "security_config.toml"

data class SecurityConfig(val authProviders: List<AuthProvider>)

data class AuthProvider(
    val name: String,
    val discoveryUrl: String,
    val acceptedAudiences: List<String> = emptyList(),
    val requitedClaims: AuthProviderRequiredClaims,
    val optionalClaims: AuthProviderOptionalClaims = AuthProviderOptionalClaims()
)

data class AuthProviderRequiredClaims(
    val claims: List<String>,
    val combineWithOr: Boolean = false
)

data class AuthProviderOptionalClaims(
    val claims: List<String> = emptyList(),
)

fun AuthProvider.asTokenSupportConfig(): TokenSupportConfig =
    TokenSupportConfig(this.asIssuerConfig())

fun AuthProvider.asIssuerConfig(): IssuerConfig =
    IssuerConfig(
        name = this.name,
        discoveryUrl = this.discoveryUrl,
        acceptedAudience = this.acceptedAudiences,
        optionalClaims = this.optionalClaims.claims
    )

fun AuthProvider.asRequiredClaims(): RequiredClaims =
    RequiredClaims(
        this.name,
        this.requitedClaims.claims.toTypedArray(),
        this.requitedClaims.combineWithOr
    )
