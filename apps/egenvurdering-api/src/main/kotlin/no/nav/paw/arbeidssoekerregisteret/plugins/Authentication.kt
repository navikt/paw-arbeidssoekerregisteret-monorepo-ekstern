package no.nav.paw.arbeidssoekerregisteret.plugins

import io.ktor.server.application.Application
import io.ktor.server.auth.authentication
import no.nav.paw.arbeidssoekerregisteret.context.ApplicationContext
import no.nav.paw.security.authentication.config.asRequiredClaims
import no.nav.paw.security.authentication.config.asTokenSupportConfig
import no.nav.security.token.support.v3.tokenValidationSupport

fun Application.configureAuthentication(applicationContext: ApplicationContext) = authentication {
    with(applicationContext) {
        securityConfig.authProviders.forEach { provider ->
            tokenValidationSupport(
                name = provider.name,
                config = provider.asTokenSupportConfig(),
                requiredClaims = provider.asRequiredClaims()
            )
        }
    }
}
