package no.nav.paw.arbeidssoekerregisteret.api.oppslag.auth

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.testing.testApplication
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.configureAuthentication
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.getNavAnsattFromToken
import no.nav.security.mock.oauth2.MockOAuth2Server

class AzureAuthenticationTest : FunSpec({

    val oauth = MockOAuth2Server()
    val testAuthUrl = "/testAuthAzure"

    beforeSpec {
        oauth.start()
    }

    afterSpec {
        oauth.shutdown()
    }

    test("For a authenticated Azure request, return OK") {
        testApplication {
            application { configureAuthentication(oauth) }
            val tokenMap =
                mapOf(
                    "oid" to "989f736f-14db-45dc-b8d1-94d621dbf2bb",
                    "NAVident" to "test"
                )
            val token =
                oauth.issueToken(
                    claims = tokenMap
                )

            routing {
                authenticate("azure") {
                    get(testAuthUrl) {
                        val navAnsatt = call.getNavAnsattFromToken()
                        navAnsatt shouldNotBe null
                        navAnsatt?.oid?.toString() shouldBe tokenMap["oid"]
                        navAnsatt?.ident shouldBe tokenMap["NAVident"]

                        call.respond(HttpStatusCode.OK)
                    }
                }
            }

            val response =
                client.get(testAuthUrl) {
                    bearerAuth(token.serialize())
                }
            response.status shouldBe HttpStatusCode.OK
        }
    }
    test("For a m2m authenticated Azure with On-Behalf-Of request, return OK") {
        testApplication {
            application { configureAuthentication(oauth) }
            val tokenMap =
                mapOf(
                    "oid" to "989f736f-14db-45dc-b8d1-94d621dbf2bb",
                    "roles" to listOf("access_as_application")
                )
            val token =
                oauth.issueToken(
                    claims = tokenMap
                )

            routing {
                authenticate("azure") {
                    get(testAuthUrl) {
                        if (call.request.headers["Nav-Ident"] != null) {
                            val navAnsatt = call.getNavAnsattFromToken()
                            navAnsatt shouldNotBe null
                            navAnsatt?.oid?.toString() shouldBe tokenMap["oid"]
                            navAnsatt?.ident shouldBe call.request.headers["Nav-Ident"]

                            call.respond(HttpStatusCode.OK)
                        } else {
                            call.respond(HttpStatusCode.BadRequest)
                        }
                    }
                }
            }

            val response =
                client.get(testAuthUrl) {
                    bearerAuth(token.serialize())
                    header("Nav-Ident", "test")
                }
            response.status shouldBe HttpStatusCode.OK
        }
    }
    test("For a m2m authenticated Azure with Client Credentials request, return OK") {
        testApplication {
            application { configureAuthentication(oauth) }
            val tokenMap =
                mapOf(
                    "roles" to listOf("access_as_application")
                )
            val token =
                oauth.issueToken(
                    claims = tokenMap
                )

            routing {
                authenticate("azure") {
                    get(testAuthUrl) {
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }

            val response =
                client.get(testAuthUrl) {
                    bearerAuth(token.serialize())
                }
            response.status shouldBe HttpStatusCode.OK
        }
    }
})
