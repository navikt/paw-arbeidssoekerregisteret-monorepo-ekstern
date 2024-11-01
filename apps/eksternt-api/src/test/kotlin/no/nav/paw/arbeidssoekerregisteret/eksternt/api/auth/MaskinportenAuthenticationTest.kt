package no.nav.paw.arbeidssoekerregisteret.eksternt.api.auth

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.testApplication
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.AuthTestData
import no.nav.security.mock.oauth2.MockOAuth2Server

class MaskinportenAuthenticationTest : FunSpec({
    val oauth = MockOAuth2Server()
    val testAuthUrl = "/testAuthMaskinporten"
    val claims = mapOf("scope" to AuthTestData.maskinportenScope)

    beforeSpec {
        oauth.start()
    }

    afterSpec {
        oauth.shutdown()
    }

    context("protected maskinporten endpoints") {
        test("Unauthenticated request should return 401") {
            testApplication {
                environment { config = MapApplicationConfig() }
                application { testAuthModule(oauth) }
                val response = client.get(testAuthUrl)

                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("Request with wrong issuer should return 401") {
            testApplication {
                environment { config = MapApplicationConfig() }
                application { testAuthModule(oauth) }
                val token =
                    oauth.issueToken(
                        issuerId = "wrong-issuer",
                        claims = claims
                    )
                val response = client.get(testAuthUrl) { bearerAuth(token.serialize()) }

                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("Request with wrong claim should return 401") {
            testApplication {
                environment { config = MapApplicationConfig() }
                application { testAuthModule(oauth) }
                val token =
                    oauth.issueToken(
                        claims = mapOf("scope" to "wrong-scope")
                    )
                val response = client.get(testAuthUrl) { bearerAuth(token.serialize()) }

                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("Request with malformed token should return 401") {
            testApplication {
                environment { config = MapApplicationConfig() }
                application { testAuthModule(oauth) }
                val token = "malformed-token"
                val response = client.get(testAuthUrl) { bearerAuth(token) }

                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("Authenticated request should return 200") {
            testApplication {
                environment { config = MapApplicationConfig() }
                application { testAuthModule(oauth) }
                val token = oauth.issueToken(claims = claims)
                val response = client.get(testAuthUrl) { bearerAuth(token.serialize()) }
                response.status shouldBe HttpStatusCode.OK
            }
        }
    }
})
