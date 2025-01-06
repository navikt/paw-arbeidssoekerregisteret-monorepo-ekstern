package no.nav.paw.arbeidssoekerregisteret.eksternt.api.routes

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.models.ArbeidssoekerperiodeResponse
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.models.EksternRequest
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.test.ApplicationTestContext
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.test.TestData
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.test.configureTestApplication
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.test.issueMaskinportenToken
import java.time.LocalDateTime
import java.time.ZoneOffset

class PeriodeRoutesTest : FreeSpec({
    with(ApplicationTestContext.withRealDataAccess()) {

        beforeSpec {
            mockOAuth2Server.start()
        }

        afterSpec {
            mockOAuth2Server.shutdown()
        }

        "Should respond with 401 Unauthorized without bearer token" {
            testApplication {
                application {
                    configureTestApplication(applicationContext, mockOAuth2Server)
                }

                val client = configureTestClient()

                val response = client.post("/api/v1/arbeidssoekerperioder") {
                    contentType(ContentType.Application.Json)
                    setBody(EksternRequest(TestData.fnr1))
                }

                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        "Should respond with 400 BadRequest" {
            testApplication {
                application {
                    configureTestApplication(applicationContext, mockOAuth2Server)
                }

                val client = configureTestClient()

                val wrongDateFormattingResponse = client.post("/api/v1/arbeidssoekerperioder") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(mockOAuth2Server.issueMaskinportenToken())
                    setBody(EksternRequest(TestData.fnr2, "01-01-2021"))
                }

                val wrongRequestBodyResponse = client.post("/api/v1/arbeidssoekerperioder") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(mockOAuth2Server.issueMaskinportenToken())
                    setBody(
                        "wrongRequestBody"
                    )
                }

                wrongDateFormattingResponse.status shouldBe HttpStatusCode.BadRequest
                wrongDateFormattingResponse.bodyAsText() shouldBe "Ugyldig dato 'fraStartetDato' må være satt med yyyy-mm-dd"

                wrongRequestBodyResponse.status shouldBe HttpStatusCode.BadRequest
                wrongRequestBodyResponse.bodyAsText() shouldBe "Ugyldig request body"
            }
        }

        "Should respond with 200 OK" {
            testApplication {
                application {
                    configureTestApplication(applicationContext, mockOAuth2Server)
                }

                periodeRepository.opprettPeriode(TestData.nyStartetPeriodeRow(identitetsnummer = TestData.fnr3))
                periodeRepository.opprettPeriode(
                    TestData.nyStartetPeriodeRow(
                        identitetsnummer = TestData.fnr3,
                        startet = LocalDateTime
                            .of(2020, 1, 1, 12, 0)
                            .toInstant(ZoneOffset.UTC)
                    )
                )

                val client = configureTestClient()
                val response = client.post("/api/v1/arbeidssoekerperioder") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(mockOAuth2Server.issueMaskinportenToken())
                    setBody(EksternRequest(TestData.fnr3))
                }
                val responseFraStartetDato = client.post("/api/v1/arbeidssoekerperioder") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(mockOAuth2Server.issueMaskinportenToken())
                    setBody(EksternRequest(TestData.fnr3, "2021-01-01"))
                }

                val responseBody = response.body<List<ArbeidssoekerperiodeResponse>>()
                response.status shouldBe HttpStatusCode.OK
                responseBody.size shouldBe 2

                val responseFraStartetDatoBody = responseFraStartetDato.body<List<ArbeidssoekerperiodeResponse>>()
                responseFraStartetDato.status shouldBe HttpStatusCode.OK
                responseFraStartetDatoBody.size shouldBe 1
            }
        }
    }
})
