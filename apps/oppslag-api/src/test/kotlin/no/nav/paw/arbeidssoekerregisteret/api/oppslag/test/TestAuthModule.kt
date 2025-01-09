package no.nav.paw.arbeidssoekerregisteret.api.oppslag.test

import io.ktor.server.application.Application
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.configureAuthentication
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.security.authentication.config.SECURITY_CONFIG
import no.nav.paw.security.authentication.config.SecurityConfig
import no.nav.security.mock.oauth2.MockOAuth2Server

fun Application.configureAuthentication(oAuth2Server: MockOAuth2Server) {
    val securityConfig = loadNaisOrLocalConfiguration<SecurityConfig>(SECURITY_CONFIG)
    val authProviders =
        securityConfig.authProviders.map {
            it.copy(
                discoveryUrl = oAuth2Server.wellKnownUrl("default").toString(),
                audiences = listOf("default")
            )
        }
    val updatedSecurityConfig = securityConfig.copy(authProviders = authProviders)
    configureAuthentication(updatedSecurityConfig)
}
