package no.nav.paw.arbeidssoekerregisteret.api.oppslag.routes

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.verify
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.auth.configureAuthentication
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ArbeidssoekerperiodeRequest
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ArbeidssoekerperiodeResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.configureHTTP
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.configureSerialization
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.ApplicationTestContext
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.TestData
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.getArbeidssoekerperiodeResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.issueAzureM2MToken
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.issueAzureToken
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.issueTokenXToken
import no.nav.paw.pdl.graphql.generated.enums.IdentGruppe
import no.nav.paw.pdl.graphql.generated.hentidenter.IdentInformasjon
import no.nav.poao_tilgang.client.Decision
import no.nav.poao_tilgang.client.PolicyRequest
import no.nav.poao_tilgang.client.PolicyResult
import no.nav.poao_tilgang.client.api.ApiResult
import java.util.*

class PerioderRoutesTest : FreeSpec({
    with(ApplicationTestContext()) {

        beforeSpec {
            mockOAuth2Server.start()
        }

        afterSpec {
            mockOAuth2Server.shutdown()
            confirmVerified(
                pdlHttpConsumerMock,
                poaoTilgangHttpClientMock,
                periodeRepositoryMock,
                opplysningerRepositoryMock,
                profileringRepositoryMock
            )
        }

        beforeTest {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns listOf(IdentInformasjon(TestData.identitetsnummer.verdi, IdentGruppe.FOLKEREGISTERIDENT))
        }

        "/arbeidssoekerperioder should respond with 401 Unauthorized without token" {
            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        perioderRoutes(authorizationService, periodeService)
                    }
                }

                val testClient = configureTestClient()

                val noPidInTokenResponse = testClient.get("api/v1/arbeidssoekerperioder")

                noPidInTokenResponse.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        "/arbeidssoekerperioder should respond with 200 OK" {
            every {
                periodeRepositoryMock.finnPerioderForIdentiteter(any<List<Identitetsnummer>>())
            } returns getArbeidssoekerperiodeResponse(TestData.periodeId)

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        perioderRoutes(authorizationService, periodeService)
                    }
                }

                val testClient = configureTestClient()

                val response = testClient.get("api/v1/arbeidssoekerperioder") {
                    bearerAuth(mockOAuth2Server.issueTokenXToken())
                }

                response.status shouldBe HttpStatusCode.OK
                val perioder = response.body<List<ArbeidssoekerperiodeResponse>>()
                perioder.size shouldBe 3

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                verify { periodeRepositoryMock.finnPerioderForIdentiteter(any<List<Identitetsnummer>>()) }
            }
        }

        "/arbeidssoekerperioder med siste-flagg should respond with 200 OK" {
            every {
                periodeRepositoryMock.finnPerioderForIdentiteter(any<List<Identitetsnummer>>())
            } returns getArbeidssoekerperiodeResponse(TestData.periodeId)

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        perioderRoutes(authorizationService, periodeService)
                    }
                }

                val testClient = configureTestClient()

                val response = testClient.get("api/v1/arbeidssoekerperioder?siste=true") {
                    bearerAuth(mockOAuth2Server.issueTokenXToken())
                }

                response.status shouldBe HttpStatusCode.OK
                val perioder = response.body<List<ArbeidssoekerperiodeResponse>>()
                perioder.size shouldBe 1
                perioder[0].periodeId shouldBe TestData.periodeId

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                verify { periodeRepositoryMock.finnPerioderForIdentiteter(any<List<Identitetsnummer>>()) }
            }
        }

        "/veileder/arbeidssoekerperioder should return 403 Forbidden uten POAO Tilgang" {
            every {
                poaoTilgangHttpClientMock.evaluatePolicies(any<List<PolicyRequest>>())
            } returns ApiResult.success(listOf(PolicyResult(UUID.randomUUID(), Decision.Deny("test", "test"))))

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        perioderRoutes(authorizationService, periodeService)
                    }
                }

                val testClient = configureTestClient()

                val response = testClient.post("api/v1/veileder/arbeidssoekerperioder") {
                    bearerAuth(mockOAuth2Server.issueAzureToken())
                    contentType(ContentType.Application.Json)
                    setBody(
                        ArbeidssoekerperiodeRequest(
                            identitetsnummer = "12345678911"
                        )
                    )
                }

                response.status shouldBe HttpStatusCode.Forbidden

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                verify { poaoTilgangHttpClientMock.evaluatePolicies(any<List<PolicyRequest>>()) }
            }
        }

        "/veileder/arbeidssoekerperioder should return 200 OK" {
            every {
                poaoTilgangHttpClientMock.evaluatePolicies(any<List<PolicyRequest>>())
            } returns ApiResult.success(listOf(PolicyResult(UUID.randomUUID(), Decision.Permit)))

            every {
                periodeRepositoryMock.finnPerioderForIdentiteter(any<List<Identitetsnummer>>())
            } returns getArbeidssoekerperiodeResponse(TestData.periodeId)

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        perioderRoutes(authorizationService, periodeService)
                    }
                }

                val testClient = configureTestClient()

                val response = testClient.post("api/v1/veileder/arbeidssoekerperioder") {
                    bearerAuth(mockOAuth2Server.issueAzureToken())
                    contentType(ContentType.Application.Json)
                    setBody(
                        ArbeidssoekerperiodeRequest(
                            identitetsnummer = "12345678911"
                        )
                    )
                }

                response.status shouldBe HttpStatusCode.OK
                val perioder = response.body<List<ArbeidssoekerperiodeResponse>>()
                perioder.size shouldBe 3

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                verify { poaoTilgangHttpClientMock.evaluatePolicies(any<List<PolicyRequest>>()) }
                verify { periodeRepositoryMock.finnPerioderForIdentiteter(any<List<Identitetsnummer>>()) }
            }
        }

        "/veileder/arbeidssoekerperioder med siste-flagg should return 200 OK" {
            every {
                poaoTilgangHttpClientMock.evaluatePolicies(any<List<PolicyRequest>>())
            } returns ApiResult.success(listOf(PolicyResult(UUID.randomUUID(), Decision.Permit)))

            every {
                periodeRepositoryMock.finnPerioderForIdentiteter(any<List<Identitetsnummer>>())
            } returns getArbeidssoekerperiodeResponse(TestData.periodeId)

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        perioderRoutes(authorizationService, periodeService)
                    }
                }

                val testClient = configureTestClient()

                val response = testClient.post("api/v1/veileder/arbeidssoekerperioder?siste=true") {
                    bearerAuth(mockOAuth2Server.issueAzureToken())
                    contentType(ContentType.Application.Json)
                    setBody(
                        ArbeidssoekerperiodeRequest(
                            identitetsnummer = "12345678911"
                        )
                    )
                }

                response.status shouldBe HttpStatusCode.OK
                val perioder = response.body<List<ArbeidssoekerperiodeResponse>>()
                perioder.size shouldBe 1

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                verify { poaoTilgangHttpClientMock.evaluatePolicies(any<List<PolicyRequest>>()) }
                verify { periodeRepositoryMock.finnPerioderForIdentiteter(any<List<Identitetsnummer>>()) }
            }
        }


        "/veileder/arbeidssoekerperioder should return 200 OK med M2M-token" {
            every {
                poaoTilgangHttpClientMock.evaluatePolicies(any<List<PolicyRequest>>())
            } returns ApiResult.success(listOf(PolicyResult(UUID.randomUUID(), Decision.Permit)))

            every {
                periodeRepositoryMock.finnPerioderForIdentiteter(any<List<Identitetsnummer>>())
            } returns getArbeidssoekerperiodeResponse(TestData.periodeId)

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        perioderRoutes(authorizationService, periodeService)
                    }
                }

                val testClient = configureTestClient()

                val response = testClient.post("api/v1/veileder/arbeidssoekerperioder") {
                    bearerAuth(mockOAuth2Server.issueAzureM2MToken())
                    contentType(ContentType.Application.Json)
                    setBody(
                        ArbeidssoekerperiodeRequest(
                            identitetsnummer = "12345678911"
                        )
                    )
                }

                response.status shouldBe HttpStatusCode.OK
                val perioder = response.body<List<ArbeidssoekerperiodeResponse>>()
                perioder.size shouldBe 3

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                verify { poaoTilgangHttpClientMock.evaluatePolicies(any<List<PolicyRequest>>()) }
                verify { periodeRepositoryMock.finnPerioderForIdentiteter(any<List<Identitetsnummer>>()) }
            }
        }
    }
})
