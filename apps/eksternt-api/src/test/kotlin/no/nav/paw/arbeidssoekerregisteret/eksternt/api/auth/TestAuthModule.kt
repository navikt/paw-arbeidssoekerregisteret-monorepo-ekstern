package no.nav.paw.arbeidssoekerregisteret.eksternt.api.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.v2.RequiredClaims
import no.nav.security.token.support.v2.tokenValidationSupport

fun Application.testAuthModule(oAuth2Server: MockOAuth2Server) {
    val applicationConfig =
        MapApplicationConfig().apply {
            put("no.nav.security.jwt.issuers.size", "1")
            put("no.nav.security.jwt.issuers.0.issuer_name", oAuth2Server.issuerUrl("default").toString())
            put("no.nav.security.jwt.issuers.0.discoveryurl", oAuth2Server.wellKnownUrl("default").toString())
            put("no.nav.security.jwt.issuers.0.validation.optional_claims", "aud,sub,nbf")
        }
    authentication {
        tokenValidationSupport(
            name = "maskinporten",
            config = applicationConfig,
            requiredClaims = RequiredClaims(oAuth2Server.issuerUrl("default").toString(), arrayOf("scope=nav:arbeid:arbeidssokerregisteret.read"))
        )
    }

    routing {
        authenticate("maskinporten") {
            route("/testAuthMaskinporten") {
                get {
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}
