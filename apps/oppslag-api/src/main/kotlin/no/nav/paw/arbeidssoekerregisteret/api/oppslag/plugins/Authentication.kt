package no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins

import io.ktor.server.application.Application
import io.ktor.server.auth.authentication
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.AuthProvider
import no.nav.security.token.support.v2.IssuerConfig
import no.nav.security.token.support.v2.RequiredClaims
import no.nav.security.token.support.v2.TokenSupportConfig
import no.nav.security.token.support.v2.tokenValidationSupport

fun Application.configureAuthentication(authProviders: List<AuthProvider>) {
    authentication {
        authProviders.forEach { authProvider ->
            tokenValidationSupport(
                name = authProvider.name,
                requiredClaims =
                    RequiredClaims(
                        authProvider.name,
                        authProvider.claims.map.toTypedArray(),
                        authProvider.claims.combineWithOr
                    ),
                config =
                    TokenSupportConfig(
                        IssuerConfig(
                            name = authProvider.name,
                            discoveryUrl = authProvider.discoveryUrl,
                            acceptedAudience = listOf(authProvider.clientId)
                        )
                    )
            )
        }
    }
}
