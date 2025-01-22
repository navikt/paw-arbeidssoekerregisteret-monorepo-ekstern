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
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.BekreftelseResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.configureHTTP
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.configureSerialization
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.ApplicationTestContext
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.TestData
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.configureAuthentication
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.issueAzureM2MToken
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.issueAzureToken
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.issueTokenXToken
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.shouldBeEqualTo
import no.nav.paw.error.model.Data
import no.nav.paw.model.Identitetsnummer
import no.nav.paw.pdl.graphql.generated.enums.IdentGruppe
import no.nav.paw.pdl.graphql.generated.hentidenter.IdentInformasjon

class BekreftelseRoutesTest : FreeSpec({

    with(ApplicationTestContext.withRealDataAccess()) {

        beforeSpec {
            mockOAuth2Server.start()
        }

        afterSpec {
            mockOAuth2Server.shutdown()
            confirmVerified(
                pdlHttpConsumerMock,
                tilgangskontrollClientMock
            )
        }

        "/arbeidssoekerbekreftelser should return 403 Forbidden without token" {
            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        bekreftelseRoutes(authorizationService, bekreftelseService)
                    }
                }

                val testClient = configureTestClient()
                val response = testClient.get("api/v1/arbeidssoekerbekreftelser")
                response.status shouldBe HttpStatusCode.Forbidden
            }
        }

        "/arbeidssoekerbekreftelser should return 403 Forbidden with Azure token" {
            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        bekreftelseRoutes(authorizationService, bekreftelseService)
                    }
                }

                val testClient = configureTestClient()
                val response = testClient.get("api/v1/arbeidssoekerbekreftelser") {
                    bearerAuth(mockOAuth2Server.issueAzureToken())
                }
                response.status shouldBe HttpStatusCode.Forbidden
            }
        }

        "/arbeidssoekerbekreftelser should return 403 Forbidden with Azure M2M token" {
            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        bekreftelseRoutes(authorizationService, bekreftelseService)
                    }
                }

                val testClient = configureTestClient()
                val response = testClient.get("api/v1/arbeidssoekerbekreftelser") {
                    bearerAuth(mockOAuth2Server.issueAzureM2MToken())
                }
                response.status shouldBe HttpStatusCode.Forbidden
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
                        bekreftelseRoutes(authorizationService, bekreftelseService)
                    }
                }

                val periode = TestData.nyStartetPeriode(identitetsnummer = TestData.fnr1)
                val bekreftelser = TestData.nyBekreftelseList(size = 3, periodeId = periode.id)
                periodeService.lagrePeriode(periode)
                bekreftelser.forEach(bekreftelseService::lagreBekreftelse)

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
                        bekreftelseRoutes(authorizationService, bekreftelseService)
                    }
                }

                val periode = TestData.nyStartetPeriode(identitetsnummer = TestData.fnr2)
                val bekreftelser = TestData.nyBekreftelseList(size = 3, periodeId = periode.id)
                periodeService.lagrePeriode(periode)
                bekreftelser.forEach(bekreftelseService::lagreBekreftelse)

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
                        bekreftelseRoutes(authorizationService, bekreftelseService)
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
                        bekreftelseRoutes(authorizationService, bekreftelseService)
                    }
                }

                val periode = TestData.nyStartetPeriode(identitetsnummer = TestData.fnr31)
                val bekreftelse = TestData.nyBekreftelse(periodeId = periode.id)
                periodeService.lagrePeriode(periode)
                bekreftelseService.lagreBekreftelse(bekreftelse)

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
                        bekreftelseRoutes(authorizationService, bekreftelseService)
                    }
                }

                val periode = TestData.nyStartetPeriode(identitetsnummer = TestData.fnr5)
                val bekreftelser = TestData.nyBekreftelseList(size = 3, periodeId = periode.id)
                periodeService.lagrePeriode(periode)
                bekreftelser.forEach(bekreftelseService::lagreBekreftelse)

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
                        bekreftelseRoutes(authorizationService, bekreftelseService)
                    }
                }

                val periode = TestData.nyStartetPeriode(identitetsnummer = TestData.fnr6)
                val bekreftelser = TestData.nyBekreftelseList(size = 3, periodeId = periode.id)
                periodeService.lagrePeriode(periode)
                bekreftelser.forEach(bekreftelseService::lagreBekreftelse)

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

        "/veileder/arbeidssoekerbekreftelser/{periodeId} should return 403 Forbidden without token" {
            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        bekreftelseRoutes(authorizationService, bekreftelseService)
                    }
                }

                val testClient = configureTestClient()

                val response = testClient.get("api/v1/veileder/arbeidssoekerbekreftelser/${TestData.periodeId1}")

                response.status shouldBe HttpStatusCode.Forbidden
            }
        }

        "/veileder/arbeidssoekerbekreftelser/{periodeId} should return 400 BadRequest if unknown periode" {
            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        bekreftelseRoutes(authorizationService, bekreftelseService)
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

        "/veileder/arbeidssoekerbekreftelser/{periodeId} should return 403 Forbidden with TokenX token" {
            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        bekreftelseRoutes(authorizationService, bekreftelseService)
                    }
                }

                val periode = TestData.nyStartetPeriode(identitetsnummer = TestData.fnr8)
                val bekreftelse = TestData.nyBekreftelse(periodeId = periode.id)
                periodeService.lagrePeriode(periode)
                bekreftelseService.lagreBekreftelse(bekreftelse)

                val testClient = configureTestClient()
                val response = testClient.get("api/v1/veileder/arbeidssoekerbekreftelser/${periode.id}") {
                    bearerAuth(mockOAuth2Server.issueTokenXToken())
                }

                response.status shouldBe HttpStatusCode.Forbidden
            }
        }

        "/veileder/arbeidssoekerbekreftelser/{periodeId} should return 403 Forbidden if no access to user" {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns TestData.nyIdentInformasjonList(TestData.fnr9)
            coEvery {
                tilgangskontrollClientMock.harAnsattTilgangTilPerson(any(), any(), any())
            } returns Data(false)

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        bekreftelseRoutes(authorizationService, bekreftelseService)
                    }
                }

                val periode = TestData.nyStartetPeriode(identitetsnummer = TestData.fnr9)
                val bekreftelse = TestData.nyBekreftelse(periodeId = periode.id)
                periodeService.lagrePeriode(periode)
                bekreftelseService.lagreBekreftelse(bekreftelse)

                val testClient = configureTestClient()
                val response = testClient.get("api/v1/veileder/arbeidssoekerbekreftelser/${periode.id}") {
                    bearerAuth(mockOAuth2Server.issueAzureToken())
                }

                response.status shouldBe HttpStatusCode.Forbidden

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                coVerify { tilgangskontrollClientMock.harAnsattTilgangTilPerson(any(), any(), any()) }
            }
        }

        "/veileder/arbeidssoekerbekreftelser/{periodeId} should return 200 OK" {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns TestData.nyIdentInformasjonList(TestData.fnr10)
            coEvery {
                tilgangskontrollClientMock.harAnsattTilgangTilPerson(any(), any(), any())
            } returns Data(true)

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        bekreftelseRoutes(authorizationService, bekreftelseService)
                    }
                }

                val periode = TestData.nyStartetPeriode(identitetsnummer = TestData.fnr10)
                val bekreftelser = TestData.nyBekreftelseList(size = 3, periodeId = periode.id)
                periodeService.lagrePeriode(periode)
                bekreftelser.forEach(bekreftelseService::lagreBekreftelse)

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
                coVerify { tilgangskontrollClientMock.harAnsattTilgangTilPerson(any(), any(), any()) }
            }
        }

        "/veileder/arbeidssoekerbekreftelser/{periodeId} med siste-flagg should return 200 OK" {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns TestData.nyIdentInformasjonList(TestData.fnr11)
            coEvery {
                tilgangskontrollClientMock.harAnsattTilgangTilPerson(any(), any(), any())
            } returns Data(true)


            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        bekreftelseRoutes(authorizationService, bekreftelseService)
                    }
                }

                val periode = TestData.nyStartetPeriode(identitetsnummer = TestData.fnr11)
                val bekreftelser = TestData.nyBekreftelseList(size = 3, periodeId = periode.id)
                periodeService.lagrePeriode(periode)
                bekreftelser.forEach(bekreftelseService::lagreBekreftelse)

                val testClient = configureTestClient()
                val response = testClient.get("api/v1/veileder/arbeidssoekerbekreftelser/${periode.id}?siste=true") {
                    bearerAuth(mockOAuth2Server.issueAzureToken())
                }

                response.status shouldBe HttpStatusCode.OK
                val bekreftelseResponses = response.body<List<BekreftelseResponse>>()
                bekreftelseResponses.size shouldBe 1
                bekreftelser[0] shouldBeEqualTo bekreftelseResponses[0]

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                coVerify { tilgangskontrollClientMock.harAnsattTilgangTilPerson(any(), any(), any()) }
            }
        }
    }
})