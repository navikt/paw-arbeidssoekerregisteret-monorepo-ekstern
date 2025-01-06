package no.nav.paw.arbeidssoekerregisteret.eksternt.api.plugins

import io.ktor.server.application.Application
import io.ktor.server.auth.authentication
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.config.SecurityConfig
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.config.asRequiredClaims
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.config.asTokenSupportConfig
import no.nav.security.token.support.v2.tokenValidationSupport

fun Application.configureAuthentication(securityConfig: SecurityConfig) {
    authentication {
        securityConfig.authProviders.forEach { provider ->
            tokenValidationSupport(
                name = provider.name,
                config = provider.asTokenSupportConfig(),
                requiredClaims = provider.asRequiredClaims(),
            )
        }
    }
}
