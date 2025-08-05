package no.nav.paw.arbeidssoekerregisteret.eksternt.api.test

import io.ktor.server.application.Application
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.context.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.plugins.configureAuthentication
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.plugins.configureHTTP
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.plugins.configureRouting
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.plugins.configureSerialization
import no.nav.paw.security.authentication.config.SecurityConfig
import no.nav.security.mock.oauth2.MockOAuth2Server

fun Application.configureTestApplication(
    applicationContext: ApplicationContext,
    mockOAuth2Server: MockOAuth2Server
) {
    with(applicationContext) {
        configureSerialization()
        configureHTTP()
        configureMockAuthentication(securityConfig, mockOAuth2Server)
        configureRouting(meterRegistry, periodeService)
    }
}

fun Application.configureMockAuthentication(
    securityConfig: SecurityConfig,
    mockOAuth2Server: MockOAuth2Server
) {

    val authProviders = securityConfig.authProviders.map {
        it.copy(
            discoveryUrl = mockOAuth2Server.wellKnownUrl("default").toString(),
        )
    }
    val updatedSecurityConfig = securityConfig.copy(authProviders = authProviders)
    configureAuthentication(updatedSecurityConfig)
}
