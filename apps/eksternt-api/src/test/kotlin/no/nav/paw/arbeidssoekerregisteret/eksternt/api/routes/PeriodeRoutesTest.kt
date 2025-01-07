package no.nav.paw.arbeidssoekerregisteret.eksternt.api.routes

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.models.ArbeidssoekerperiodeResponse
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.models.EksternRequest
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.models.asArbeidssoekerperiodeResponse
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

        "Should respond with 403 Forbidden without bearer token" {
            testApplication {
                application {
                    configureTestApplication(applicationContext, mockOAuth2Server)
                }

                val client = configureTestClient()

                val response = client.post("/api/v1/arbeidssoekerperioder") {
                    contentType(ContentType.Application.Json)
                    setBody(EksternRequest(TestData.fnr1))
                }

                response.status shouldBe HttpStatusCode.Forbidden
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
                wrongRequestBodyResponse.status shouldBe HttpStatusCode.BadRequest
            }
        }

        "Should respond with 200 OK" {
            testApplication {
                application {
                    configureTestApplication(applicationContext, mockOAuth2Server)
                }
                val periode1 = TestData.nyStartetPeriodeRow(identitetsnummer = TestData.fnr3)
                val periode2 = TestData.nyStartetPeriodeRow(
                    identitetsnummer = TestData.fnr3,
                    startet = LocalDateTime
                        .of(2020, 1, 1, 12, 0)
                        .toInstant(ZoneOffset.UTC)
                )

                periodeRepository.opprettPeriode(periode1)
                periodeRepository.opprettPeriode(periode2)

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
                responseBody shouldContain periode1.asArbeidssoekerperiodeResponse()
                responseBody shouldContain periode2.asArbeidssoekerperiodeResponse()

                val responseFraStartetDatoBody = responseFraStartetDato.body<List<ArbeidssoekerperiodeResponse>>()
                responseFraStartetDato.status shouldBe HttpStatusCode.OK
                responseFraStartetDatoBody.size shouldBe 1
                responseFraStartetDatoBody shouldContain periode1.asArbeidssoekerperiodeResponse()
            }
        }
    }
})
