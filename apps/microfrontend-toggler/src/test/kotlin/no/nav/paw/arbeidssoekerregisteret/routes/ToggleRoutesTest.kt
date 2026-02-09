package no.nav.paw.arbeidssoekerregisteret.routes

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.just
import no.nav.paw.arbeidssoekerregisteret.model.MicroFrontend
import no.nav.paw.arbeidssoekerregisteret.model.Toggle
import no.nav.paw.arbeidssoekerregisteret.model.ToggleAction
import no.nav.paw.arbeidssoekerregisteret.model.ToggleRequest
import no.nav.paw.arbeidssoekerregisteret.test.TestContext
import no.nav.paw.arbeidssoekerregisteret.test.issueTokenXToken
import no.nav.paw.error.model.ProblemDetails
import no.nav.paw.security.authorization.exception.IngenTilgangException
import no.nav.paw.security.authorization.exception.UgyldigBearerTokenException

class ToggleRoutesTest : FreeSpec({
    with(LocalTestContext()) {

        beforeSpec {
            mockOAuth2Server.start()
        }

        afterSpec {
            mockOAuth2Server.shutdown()
            confirmVerified(
                toggleServiceMock
            )
        }

        "Test av toggle routes" - {

            "Skal få 403 ved manglende Bearer Token" {
                testApplication {
                    configureTestApplication()
                    val client = configureTestClient()

                    val response = client.post("/api/v1/microfrontend-toggle") {
                        contentType(ContentType.Application.Json)
                        setBody(ToggleRequest(ToggleAction.DISABLE, MicroFrontend.AIA_MIN_SIDE))
                    }

                    response.status shouldBe HttpStatusCode.Forbidden
                }
            }

            "Skal få 403 ved token utstedt av ukjent issuer" {
                testApplication {
                    configureTestApplication()
                    val client = configureTestClient()

                    val response = client.post("/api/v1/microfrontend-toggle") {
                        bearerAuth(mockOAuth2Server.issueTokenXToken(issuerId = "whatever"))
                        contentType(ContentType.Application.Json)
                        setBody(ToggleRequest(ToggleAction.DISABLE, MicroFrontend.AIA_MIN_SIDE))
                    }

                    response.status shouldBe HttpStatusCode.Forbidden
                }
            }

            "Skal få 403 ved token uten noen claims" {
                testApplication {
                    configureTestApplication()
                    val client = configureTestClient()

                    val response = client.post("/api/v1/microfrontend-toggle") {
                        bearerAuth(mockOAuth2Server.issueToken().serialize())
                        contentType(ContentType.Application.Json)
                        setBody(ToggleRequest(ToggleAction.DISABLE, MicroFrontend.AIA_MIN_SIDE))
                    }

                    response.status shouldBe HttpStatusCode.Forbidden
                }
            }

            "Skal få 403 ved token uten pid claim" {
                testApplication {
                    configureTestApplication()
                    val client = configureTestClient()

                    val token = mockOAuth2Server.issueToken(
                        claims = mapOf(
                            "acr" to "idporten-loa-high"
                        )
                    )

                    val response = client.post("/api/v1/microfrontend-toggle") {
                        bearerAuth(token.serialize())
                        contentType(ContentType.Application.Json)
                        setBody(ToggleRequest(ToggleAction.DISABLE, MicroFrontend.AIA_MIN_SIDE))
                    }

                    response.status shouldBe HttpStatusCode.Forbidden
                    val body = response.body<ProblemDetails>()
                    body.status shouldBe HttpStatusCode.Forbidden
                    body.type shouldBe UgyldigBearerTokenException("").type
                }
            }

            "Skal få 403 ved ved forsøk på å enable" {
                testApplication {
                    configureTestApplication()
                    val client = configureTestClient()

                    val response = client.post("/api/v1/microfrontend-toggle") {
                        bearerAuth(mockOAuth2Server.issueTokenXToken())
                        contentType(ContentType.Application.Json)
                        setBody(ToggleRequest(ToggleAction.ENABLE, MicroFrontend.AIA_MIN_SIDE))
                    }

                    response.status shouldBe HttpStatusCode.Forbidden
                    val body = response.body<ProblemDetails>()
                    body.status shouldBe HttpStatusCode.Forbidden
                    body.type shouldBe IngenTilgangException("").type
                }
            }

            "Skal få 202 ved ved forsøk på å disable" {
                coEvery { toggleServiceMock.sendToggle(any<Toggle>()) } just Runs

                testApplication {
                    configureTestApplication()
                    val client = configureTestClient()

                    val response = client.post("/api/v1/microfrontend-toggle") {
                        bearerAuth(mockOAuth2Server.issueTokenXToken())
                        contentType(ContentType.Application.Json)
                        setBody(ToggleRequest(ToggleAction.DISABLE, MicroFrontend.AIA_MIN_SIDE))
                    }

                    response.status shouldBe HttpStatusCode.Accepted
                }

                coVerify { toggleServiceMock.sendToggle(any<Toggle>()) }
            }
        }
    }
}) {
    private class LocalTestContext : TestContext()
}
