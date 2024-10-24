package no.nav.paw.arbeidssoekerregisteret.eksternt.api.plugins

import io.ktor.server.application.Application
import io.ktor.server.auth.authentication
import io.ktor.server.config.ApplicationConfig
import no.nav.security.token.support.v2.RequiredClaims
import no.nav.security.token.support.v2.tokenValidationSupport

fun Application.configureAuthentication(applicationConfig: ApplicationConfig) {
    val issuer =
        applicationConfig
            .configList("no.nav.security.jwt.issuers")
            .first()
            .property("issuer_name")
            .getString()

    authentication {
        tokenValidationSupport(
            name = "maskinporten",
            config = applicationConfig,
            requiredClaims = RequiredClaims(issuer, arrayOf("scope=nav:arbeid:arbeidssokerregisteret.read"))
        )
    }
}
