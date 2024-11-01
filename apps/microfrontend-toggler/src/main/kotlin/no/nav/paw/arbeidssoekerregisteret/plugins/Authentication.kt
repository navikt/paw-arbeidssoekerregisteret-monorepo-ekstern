package no.nav.paw.arbeidssoekerregisteret.plugins

import io.ktor.server.application.Application
import io.ktor.server.auth.authentication
import no.nav.paw.arbeidssoekerregisteret.context.ApplicationContext
import no.nav.security.token.support.v2.IssuerConfig
import no.nav.security.token.support.v2.RequiredClaims
import no.nav.security.token.support.v2.TokenSupportConfig
import no.nav.security.token.support.v2.tokenValidationSupport

fun Application.configureAuthentication(applicationContext: ApplicationContext) = authentication {
    with(applicationContext) {
        securityConfig.authProviders.forEach { provider ->
            tokenValidationSupport(
                name = provider.name,
                requiredClaims = RequiredClaims(
                    provider.name,
                    provider.claims.map.toTypedArray(),
                    provider.claims.combineWithOr
                ),
                config = TokenSupportConfig(
                    IssuerConfig(
                        name = provider.name,
                        discoveryUrl = provider.discoveryUrl,
                        acceptedAudience = listOf(provider.clientId)
                    )
                )
            )
        }
    }
}
