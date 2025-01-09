package no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins

import io.ktor.server.application.Application
import io.ktor.server.auth.authentication
import no.nav.paw.security.authentication.config.SecurityConfig
import no.nav.paw.security.authentication.config.asRequiredClaims
import no.nav.paw.security.authentication.config.asTokenSupportConfig
import no.nav.security.token.support.v2.tokenValidationSupport

fun Application.configureAuthentication(securityConfig: SecurityConfig) {
    authentication {
        securityConfig.authProviders.forEach { provider ->
            tokenValidationSupport(
                name = provider.name,
                config = provider.asTokenSupportConfig(),
                requiredClaims = provider.asRequiredClaims()
            )
        }
    }
}
