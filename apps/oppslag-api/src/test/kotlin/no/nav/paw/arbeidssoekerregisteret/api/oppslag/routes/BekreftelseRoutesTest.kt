package no.nav.paw.arbeidssoekerregisteret.api.oppslag.routes

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.auth.configureAuthentication
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.BekreftelseResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.configureHTTP
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.configureSerialization
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.ApplicationTestContext
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.TestData
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.issueAzureToken
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.issueTokenXToken
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.shouldBeEqualTo
import no.nav.paw.pdl.graphql.generated.enums.IdentGruppe
import no.nav.paw.pdl.graphql.generated.hentidenter.IdentInformasjon
import no.nav.poao_tilgang.client.Decision
import no.nav.poao_tilgang.client.PolicyRequest
import no.nav.poao_tilgang.client.PolicyResult
import no.nav.poao_tilgang.client.api.ApiResult
import java.util.*

class BekreftelseRoutesTest : FreeSpec({

    with(ApplicationTestContext.withRealDataAccess()) {

        beforeSpec {
            mockOAuth2Server.start()
        }

        afterSpec {
            mockOAuth2Server.shutdown()
            confirmVerified(
                pdlHttpConsumerMock,
                poaoTilgangHttpClientMock
            )
        }

        "/arbeidssoekerbekreftelser/{periodeId} should return 401 Unauthorized without token" {
            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        bekreftelseRoutes(authorizationService, bekreftelseService, periodeService)
                    }
                }

                val testClient = configureTestClient()

                val response = testClient
                    .get("api/v1/arbeidssoekerbekreftelser/${TestData.periodeId1}")

                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        "/arbeidssoekerbekreftelser should return 200 OK" {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns listOf(IdentInformasjon(TestData.fnr1, IdentGruppe.FOLKEREGISTERIDENT))

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        bekreftelseRoutes(authorizationService, bekreftelseService, periodeService)
                    }
                }

                val periode = TestData.nyStartetPeriode(identitetsnummer = TestData.fnr1)
                val bekreftelser = TestData.nyBekreftelseList(size = 3, periodeId = periode.id)
                periodeService.lagreAllePerioder(listOf(periode).asSequence())
                bekreftelseService.lagreAlleBekreftelser(bekreftelser.asSequence())

                val testClient = configureTestClient()

                val response = testClient.get("api/v1/arbeidssoekerbekreftelser") {
                    bearerAuth(mockOAuth2Server.issueTokenXToken(pid = periode.identitetsnummer))
                }

                response.status shouldBe HttpStatusCode.OK
                val bekreftelseResponses = response.body<List<BekreftelseResponse>>()
                bekreftelseResponses.size shouldBe 3
                bekreftelser[0] shouldBeEqualTo bekreftelseResponses[0]
                bekreftelser[1] shouldBeEqualTo bekreftelseResponses[1]
                bekreftelser[2] shouldBeEqualTo bekreftelseResponses[2]

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
            }
        }

        "/arbeidssoekerbekreftelser med siste-flagg should return 200 OK" {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns listOf(IdentInformasjon(TestData.fnr2, IdentGruppe.FOLKEREGISTERIDENT))

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        bekreftelseRoutes(authorizationService, bekreftelseService, periodeService)
                    }
                }

                val periode = TestData.nyStartetPeriode(identitetsnummer = TestData.fnr2)
                val bekreftelser = TestData.nyBekreftelseList(size = 3, periodeId = periode.id)
                periodeService.lagreAllePerioder(listOf(periode).asSequence())
                bekreftelseService.lagreAlleBekreftelser(bekreftelser.asSequence())

                val testClient = configureTestClient()

                val response = testClient.get("api/v1/arbeidssoekerbekreftelser?siste=true") {
                    bearerAuth(mockOAuth2Server.issueTokenXToken(pid = periode.identitetsnummer))
                }

                response.status shouldBe HttpStatusCode.OK
                val bekreftelseResponses = response.body<List<BekreftelseResponse>>()
                bekreftelseResponses.size shouldBe 1
                bekreftelser[0] shouldBeEqualTo bekreftelseResponses[0]

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
            }
        }

        "/arbeidssoekerbekreftelser/{periodeId} should return 400 BadRequest if unknown periode" {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns listOf(IdentInformasjon(TestData.fnr3, IdentGruppe.FOLKEREGISTERIDENT))

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        bekreftelseRoutes(authorizationService, bekreftelseService, periodeService)
                    }
                }

                val testClient = configureTestClient()

                val response = testClient.get("api/v1/arbeidssoekerbekreftelser/${TestData.periodeId3}") {
                    bearerAuth(mockOAuth2Server.issueTokenXToken(pid = TestData.fnr3))
                }

                response.status shouldBe HttpStatusCode.BadRequest

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
            }
        }

        "/arbeidssoekerbekreftelser/{periodeId} should return 403 Forbidden if periode not owned by user" {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns listOf(IdentInformasjon(TestData.fnr4, IdentGruppe.FOLKEREGISTERIDENT))

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        bekreftelseRoutes(authorizationService, bekreftelseService, periodeService)
                    }
                }

                val periode = TestData.nyStartetPeriode(identitetsnummer = TestData.fnr31)
                val bekreftelse = TestData.nyBekreftelse(periodeId = periode.id)
                periodeService.lagreAllePerioder(listOf(periode).asSequence())
                bekreftelseService.lagreAlleBekreftelser(listOf(bekreftelse).asSequence())

                val testClient = configureTestClient()

                val response = testClient.get("api/v1/arbeidssoekerbekreftelser/${periode.id}") {
                    bearerAuth(mockOAuth2Server.issueTokenXToken(pid = TestData.fnr4))
                }

                response.status shouldBe HttpStatusCode.Forbidden

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
            }
        }

        "/arbeidssoekerbekreftelser/{periodeId} should return 200 OK" {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns listOf(IdentInformasjon(TestData.fnr5, IdentGruppe.FOLKEREGISTERIDENT))

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        bekreftelseRoutes(authorizationService, bekreftelseService, periodeService)
                    }
                }

                val periode = TestData.nyStartetPeriode(identitetsnummer = TestData.fnr5)
                val bekreftelser = TestData.nyBekreftelseList(size = 3, periodeId = periode.id)
                periodeService.lagreAllePerioder(listOf(periode).asSequence())
                bekreftelseService.lagreAlleBekreftelser(bekreftelser.asSequence())

                val testClient = configureTestClient()

                val response = testClient.get("api/v1/arbeidssoekerbekreftelser/${periode.id}") {
                    bearerAuth(mockOAuth2Server.issueTokenXToken(pid = periode.identitetsnummer))
                }

                response.status shouldBe HttpStatusCode.OK
                val bekreftelseResponses = response.body<List<BekreftelseResponse>>()
                bekreftelseResponses.size shouldBe 3
                bekreftelser[0] shouldBeEqualTo bekreftelseResponses[0]
                bekreftelser[1] shouldBeEqualTo bekreftelseResponses[1]
                bekreftelser[2] shouldBeEqualTo bekreftelseResponses[2]

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
            }
        }

        "/arbeidssoekerbekreftelser/{periodeId} med siste-flagg should return 200 OK" {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns listOf(IdentInformasjon(TestData.fnr6, IdentGruppe.FOLKEREGISTERIDENT))

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        bekreftelseRoutes(authorizationService, bekreftelseService, periodeService)
                    }
                }

                val periode = TestData.nyStartetPeriode(identitetsnummer = TestData.fnr6)
                val bekreftelser = TestData.nyBekreftelseList(size = 3, periodeId = periode.id)
                periodeService.lagreAllePerioder(listOf(periode).asSequence())
                bekreftelseService.lagreAlleBekreftelser(bekreftelser.asSequence())

                val testClient = configureTestClient()

                val response = testClient.get("api/v1/arbeidssoekerbekreftelser/${periode.id}?siste=true") {
                    bearerAuth(mockOAuth2Server.issueTokenXToken(pid = periode.identitetsnummer))
                }

                response.status shouldBe HttpStatusCode.OK
                val bekreftelseResponses = response.body<List<BekreftelseResponse>>()
                bekreftelseResponses.size shouldBe 1
                bekreftelser[0] shouldBeEqualTo bekreftelseResponses[0]

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
            }
        }

        "/veileder/arbeidssoekerbekreftelser/{periodeId} should return 401 Unauthorized without token" {
            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        bekreftelseRoutes(authorizationService, bekreftelseService, periodeService)
                    }
                }

                val testClient = configureTestClient()

                val response = testClient
                    .get("api/v1/veileder/arbeidssoekerbekreftelser/${TestData.periodeId1}")

                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        "/veileder/arbeidssoekerbekreftelser/{periodeId} should return 400 BadRequest if unknown periode" {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns listOf(IdentInformasjon(TestData.fnr7, IdentGruppe.FOLKEREGISTERIDENT))

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        bekreftelseRoutes(authorizationService, bekreftelseService, periodeService)
                    }
                }

                val testClient = configureTestClient()

                val response = testClient.get("api/v1/veileder/arbeidssoekerbekreftelser/${TestData.periodeId3}") {
                    bearerAuth(mockOAuth2Server.issueAzureToken())
                }

                response.status shouldBe HttpStatusCode.BadRequest

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
            }
        }

        "/veileder/arbeidssoekerbekreftelser/{periodeId} should return 403 Forbidden is no access to user" {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns listOf(IdentInformasjon(TestData.fnr8, IdentGruppe.FOLKEREGISTERIDENT))
            every {
                poaoTilgangHttpClientMock.evaluatePolicies(any<List<PolicyRequest>>())
            } returns ApiResult.success(
                listOf(
                    PolicyResult(UUID.randomUUID(), Decision.Deny("test", "test")),
                    PolicyResult(UUID.randomUUID(), Decision.Permit)
                )
            )

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        bekreftelseRoutes(authorizationService, bekreftelseService, periodeService)
                    }
                }

                val periode = TestData.nyStartetPeriode(identitetsnummer = TestData.fnr8)
                val bekreftelse = TestData.nyBekreftelse(periodeId = periode.id)
                periodeService.lagreAllePerioder(listOf(periode).asSequence())
                bekreftelseService.lagreAlleBekreftelser(listOf(bekreftelse).asSequence())

                val testClient = configureTestClient()

                val response = testClient.get("api/v1/veileder/arbeidssoekerbekreftelser/${periode.id}") {
                    bearerAuth(mockOAuth2Server.issueAzureToken())
                }

                response.status shouldBe HttpStatusCode.Forbidden

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                coVerify { poaoTilgangHttpClientMock.evaluatePolicies(any<List<PolicyRequest>>()) }
            }
        }

        "/veileder/arbeidssoekerbekreftelser/{periodeId} should return 200 OK" {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns listOf(IdentInformasjon(TestData.fnr9, IdentGruppe.FOLKEREGISTERIDENT))
            every {
                poaoTilgangHttpClientMock.evaluatePolicies(any<List<PolicyRequest>>())
            } returns ApiResult.success(
                listOf(
                    PolicyResult(UUID.randomUUID(), Decision.Permit),
                    PolicyResult(UUID.randomUUID(), Decision.Permit)
                )
            )

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        bekreftelseRoutes(authorizationService, bekreftelseService, periodeService)
                    }
                }

                val periode = TestData.nyStartetPeriode(identitetsnummer = TestData.fnr9)
                val bekreftelser = TestData.nyBekreftelseList(size = 3, periodeId = periode.id)
                periodeService.lagreAllePerioder(listOf(periode).asSequence())
                bekreftelseService.lagreAlleBekreftelser(bekreftelser.asSequence())

                val testClient = configureTestClient()

                val response = testClient.get("api/v1/veileder/arbeidssoekerbekreftelser/${periode.id}") {
                    bearerAuth(mockOAuth2Server.issueAzureToken())
                }

                response.status shouldBe HttpStatusCode.OK
                val bekreftelseResponses = response.body<List<BekreftelseResponse>>()
                bekreftelseResponses.size shouldBe 3
                bekreftelser[0] shouldBeEqualTo bekreftelseResponses[0]
                bekreftelser[1] shouldBeEqualTo bekreftelseResponses[1]
                bekreftelser[2] shouldBeEqualTo bekreftelseResponses[2]

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                coVerify { poaoTilgangHttpClientMock.evaluatePolicies(any<List<PolicyRequest>>()) }
            }
        }

        "/veileder/arbeidssoekerbekreftelser/{periodeId} med siste-flagg should return 200 OK" {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns listOf(IdentInformasjon(TestData.fnr10, IdentGruppe.FOLKEREGISTERIDENT))
            every {
                poaoTilgangHttpClientMock.evaluatePolicies(any<List<PolicyRequest>>())
            } returns ApiResult.success(
                listOf(
                    PolicyResult(UUID.randomUUID(), Decision.Permit),
                    PolicyResult(UUID.randomUUID(), Decision.Permit)
                )
            )

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        bekreftelseRoutes(authorizationService, bekreftelseService, periodeService)
                    }
                }

                val periode = TestData.nyStartetPeriode(identitetsnummer = TestData.fnr10)
                val bekreftelser = TestData.nyBekreftelseList(size = 3, periodeId = periode.id)
                periodeService.lagreAllePerioder(listOf(periode).asSequence())
                bekreftelseService.lagreAlleBekreftelser(bekreftelser.asSequence())

                val testClient = configureTestClient()

                val response = testClient.get("api/v1/veileder/arbeidssoekerbekreftelser/${periode.id}?siste=true") {
                    bearerAuth(mockOAuth2Server.issueAzureToken())
                }

                response.status shouldBe HttpStatusCode.OK
                val bekreftelseResponses = response.body<List<BekreftelseResponse>>()
                bekreftelseResponses.size shouldBe 1
                bekreftelser[0] shouldBeEqualTo bekreftelseResponses[0]

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                coVerify { poaoTilgangHttpClientMock.evaluatePolicies(any<List<PolicyRequest>>()) }
            }
        }
    }
})