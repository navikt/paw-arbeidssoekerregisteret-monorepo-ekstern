package no.nav.paw.arbeidssoekerregisteret.api.oppslag.auth

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.getPidClaim
import no.nav.security.mock.oauth2.MockOAuth2Server

class TokenXAuthenticationTest : FunSpec({
    val oauth = MockOAuth2Server()
    val testAuthUrl = "/testAuthTokenx"

    beforeSpec {
        oauth.start()
    }

    afterSpec {
        oauth.shutdown()
    }

    test("For an authenticated tokenx request, return OK") {
        testApplication {
            application { configureAuthentication(oauth) }

            val tokenMap =
                mapOf(
                    "acr" to "idporten-loa-high",
                    "pid" to "12345678901"
                )

            val token =
                oauth.issueToken(
                    claims = tokenMap
                )

            routing {
                authenticate("tokenx") {
                    get(testAuthUrl) {
                        call.getPidClaim().verdi shouldBe tokenMap["pid"]
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }

            val response = client.get(testAuthUrl) { bearerAuth(token.serialize()) }
            response.status shouldBe HttpStatusCode.OK
        }
    }
})
