package no.nav.paw.oppslagsapi

import no.nav.paw.security.authentication.config.AuthProvider
import no.nav.paw.security.authentication.config.AuthProviderRequiredClaims
import no.nav.paw.security.authentication.model.AzureAd
import no.nav.paw.security.authentication.model.IdPorten
import no.nav.paw.security.authentication.model.TokenX
import no.nav.security.mock.oauth2.MockOAuth2Server

fun MockOAuth2Server.createAuthProviders(): List<AuthProvider> {
    val wellKnownUrl = wellKnownUrl("default").toString()
    return listOf(
        AuthProvider(
            name = IdPorten.name,
            audiences = listOf("default"),
            discoveryUrl = wellKnownUrl,
            requiredClaims = AuthProviderRequiredClaims(listOf("acr=idporten-loa-high"))
        ),
        AuthProvider(
            name = TokenX.name,
            audiences = listOf("default"),
            discoveryUrl = wellKnownUrl,
            requiredClaims = AuthProviderRequiredClaims(listOf("acr=Level4", "acr=idporten-loa-high"), true)
        ),
        AuthProvider(
            name = AzureAd.name,
            audiences = listOf("default"),
            discoveryUrl = wellKnownUrl,
            requiredClaims = AuthProviderRequiredClaims(listOf("NAVident"))
        )
    )
}