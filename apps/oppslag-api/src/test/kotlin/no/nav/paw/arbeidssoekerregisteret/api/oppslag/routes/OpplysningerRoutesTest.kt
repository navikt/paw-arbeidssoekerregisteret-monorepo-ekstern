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
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.OpplysningerOmArbeidssoekerRequest
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.OpplysningerOmArbeidssoekerResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.configureHTTP
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.configureSerialization
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.ApplicationTestContext
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.TestData
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.configureAuthentication
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.issueAzureM2MToken
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.issueAzureToken
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.issueTokenXToken
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.shouldBeEqualTo
import no.nav.paw.pdl.graphql.generated.enums.IdentGruppe
import no.nav.paw.pdl.graphql.generated.hentidenter.IdentInformasjon
import no.nav.paw.security.authentication.model.Identitetsnummer
import no.nav.poao_tilgang.client.Decision
import no.nav.poao_tilgang.client.PolicyRequest
import no.nav.poao_tilgang.client.PolicyResult
import java.util.*

class OpplysningerRoutesTest : FreeSpec({
    with(ApplicationTestContext.withRealDataAccess()) {

        beforeSpec {
            mockOAuth2Server.start()
        }

        afterSpec {
            mockOAuth2Server.shutdown()
            confirmVerified(
                pdlHttpConsumerMock,
                poaoTilgangHttpConsumerMock
            )
        }

        "/opplysninger-om-arbeidssoeker should return 401 Unauthorized without token" {
            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        opplysningerRoutes(authorizationService, periodeService, opplysningerService)
                    }
                }

                val testClient = configureTestClient()

                val response = testClient.get("api/v1/opplysninger-om-arbeidssoeker")

                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        "/opplysninger-om-arbeidssoeker should return OK" {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns listOf(IdentInformasjon(TestData.fnr1, IdentGruppe.FOLKEREGISTERIDENT))

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        opplysningerRoutes(authorizationService, periodeService, opplysningerService)
                    }
                }

                val periode = TestData.nyStartetPeriode(identitetsnummer = TestData.fnr1)
                val opplysninger = TestData.nyOpplysningerOmArbeidssoekerList(size = 3, periodeId = periode.id)
                periodeService.lagreAllePerioder(listOf(periode))
                opplysningerService.lagreAlleOpplysninger(opplysninger)

                val testClient = configureTestClient()

                val response = testClient.get("api/v1/opplysninger-om-arbeidssoeker") {
                    bearerAuth(mockOAuth2Server.issueTokenXToken(pid = periode.identitetsnummer))
                }

                response.status shouldBe HttpStatusCode.OK
                val opplysningerResponses = response.body<List<OpplysningerOmArbeidssoekerResponse>>()
                opplysningerResponses.size shouldBe 3
                opplysninger[0] shouldBeEqualTo opplysningerResponses[0]
                opplysninger[1] shouldBeEqualTo opplysningerResponses[1]
                opplysninger[2] shouldBeEqualTo opplysningerResponses[2]

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
            }
        }

        "/opplysninger-om-arbeidssoeker med siste-flagg should return OK" {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns listOf(IdentInformasjon(TestData.fnr2, IdentGruppe.FOLKEREGISTERIDENT))

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        opplysningerRoutes(authorizationService, periodeService, opplysningerService)
                    }
                }

                val periode = TestData.nyStartetPeriode(identitetsnummer = TestData.fnr2)
                val opplysninger = TestData.nyOpplysningerOmArbeidssoekerList(size = 3, periodeId = periode.id)
                periodeService.lagreAllePerioder(listOf(periode))
                opplysningerService.lagreAlleOpplysninger(opplysninger)

                val testClient = configureTestClient()

                val response = testClient.get("api/v1/opplysninger-om-arbeidssoeker?siste=true") {
                    bearerAuth(mockOAuth2Server.issueTokenXToken(pid = periode.identitetsnummer))
                }

                response.status shouldBe HttpStatusCode.OK
                val opplysningerResponses = response.body<List<OpplysningerOmArbeidssoekerResponse>>()
                opplysningerResponses.size shouldBe 1
                opplysninger[0] shouldBeEqualTo opplysningerResponses[0]

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
            }
        }

        "/opplysninger-om-arbeidssoeker/{periodeId} should return 400 BadRequest if periode does not exist for periodeId" {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns listOf(IdentInformasjon(TestData.fnr3, IdentGruppe.FOLKEREGISTERIDENT))

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        opplysningerRoutes(authorizationService, periodeService, opplysningerService)
                    }
                }

                val testClient = configureTestClient()

                val response = testClient.get("api/v1/opplysninger-om-arbeidssoeker/${TestData.periodeId1}") {
                    bearerAuth(mockOAuth2Server.issueTokenXToken(pid = TestData.fnr3))
                }

                response.status shouldBe HttpStatusCode.BadRequest

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
            }
        }

        "/opplysninger-om-arbeidssoeker/{periodeId} should return 403 Forbidden if periodeId does not exist for user" {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns listOf(IdentInformasjon(TestData.fnr4, IdentGruppe.FOLKEREGISTERIDENT))

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        opplysningerRoutes(authorizationService, periodeService, opplysningerService)
                    }
                }

                val periode = TestData.nyStartetPeriode(identitetsnummer = TestData.fnr1)
                periodeService.lagreAllePerioder(listOf(periode))

                val testClient = configureTestClient()

                val response = testClient.get("api/v1/opplysninger-om-arbeidssoeker/${periode.id}") {
                    bearerAuth(mockOAuth2Server.issueTokenXToken(pid = TestData.fnr4))
                }

                response.status shouldBe HttpStatusCode.Forbidden

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
            }
        }

        "/opplysninger-om-arbeidssoeker/{periodeId} should return OK" {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns listOf(IdentInformasjon(TestData.fnr5, IdentGruppe.FOLKEREGISTERIDENT))

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        opplysningerRoutes(authorizationService, periodeService, opplysningerService)
                    }
                }

                val periode = TestData.nyStartetPeriode(identitetsnummer = TestData.fnr5)
                val opplysninger = TestData.nyOpplysningerOmArbeidssoekerList(periodeId = periode.id)
                periodeService.lagreAllePerioder(listOf(periode))
                opplysningerService.lagreAlleOpplysninger(opplysninger)

                val testClient = configureTestClient()

                val response = testClient.get("api/v1/opplysninger-om-arbeidssoeker/${periode.id}") {
                    bearerAuth(mockOAuth2Server.issueTokenXToken())
                }

                response.status shouldBe HttpStatusCode.OK
                val opplysningerResponses = response.body<List<OpplysningerOmArbeidssoekerResponse>>()
                opplysningerResponses.size shouldBe 1
                opplysninger[0] shouldBeEqualTo opplysningerResponses[0]

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
            }
        }

        "/veileder/opplysninger-om-arbeidssoeker should return 403 Forbidden uten POAO Tilgang" {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns listOf(IdentInformasjon(TestData.fnr6, IdentGruppe.FOLKEREGISTERIDENT))
            every {
                poaoTilgangHttpConsumerMock.evaluatePolicies(any<List<PolicyRequest>>())
            } returns listOf(
                PolicyResult(UUID.randomUUID(), Decision.Deny("test", "test")),
                PolicyResult(UUID.randomUUID(), Decision.Permit)
            )

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        opplysningerRoutes(authorizationService, periodeService, opplysningerService)
                    }
                }

                val testClient = configureTestClient()

                val response = testClient.post("api/v1/veileder/opplysninger-om-arbeidssoeker") {
                    bearerAuth(mockOAuth2Server.issueAzureToken())
                    contentType(ContentType.Application.Json)
                    setBody(
                        OpplysningerOmArbeidssoekerRequest(
                            identitetsnummer = TestData.fnr6, periodeId = TestData.periodeId1
                        )
                    )
                }

                response.status shouldBe HttpStatusCode.Forbidden

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                verify { poaoTilgangHttpConsumerMock.evaluatePolicies(any<List<PolicyRequest>>()) }
            }
        }

        "/veileder/opplysninger-om-arbeidssoeker should return 403 Forbidden når periode ikke tilhører sluttbruker" {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns listOf(IdentInformasjon(TestData.fnr7, IdentGruppe.FOLKEREGISTERIDENT))
            every {
                poaoTilgangHttpConsumerMock.evaluatePolicies(any<List<PolicyRequest>>())
            } returns listOf(PolicyResult(UUID.randomUUID(), Decision.Permit))

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        opplysningerRoutes(authorizationService, periodeService, opplysningerService)
                    }
                }

                val periode = TestData.nyStartetPeriode(identitetsnummer = TestData.fnr1)
                periodeService.lagreAllePerioder(listOf(periode))

                val testClient = configureTestClient()

                val response = testClient.post("api/v1/veileder/opplysninger-om-arbeidssoeker") {
                    bearerAuth(mockOAuth2Server.issueAzureToken())
                    contentType(ContentType.Application.Json)
                    setBody(
                        OpplysningerOmArbeidssoekerRequest(
                            identitetsnummer = TestData.fnr7, periodeId = periode.id
                        )
                    )
                }

                response.status shouldBe HttpStatusCode.Forbidden

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                verify { poaoTilgangHttpConsumerMock.evaluatePolicies(any<List<PolicyRequest>>()) }
            }
        }

        "/veileder/opplysninger-om-arbeidssoeker should return 200 OK" {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns listOf(IdentInformasjon(TestData.fnr8, IdentGruppe.FOLKEREGISTERIDENT))
            every {
                poaoTilgangHttpConsumerMock.evaluatePolicies(any<List<PolicyRequest>>())
            } returns listOf(
                PolicyResult(UUID.randomUUID(), Decision.Permit),
                PolicyResult(UUID.randomUUID(), Decision.Permit)
            )

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        opplysningerRoutes(authorizationService, periodeService, opplysningerService)
                    }
                }

                val periode = TestData.nyStartetPeriode(identitetsnummer = TestData.fnr8)
                val opplysninger = TestData.nyOpplysningerOmArbeidssoekerList(size = 3, periodeId = periode.id)
                periodeService.lagreAllePerioder(listOf(periode))
                opplysningerService.lagreAlleOpplysninger(opplysninger)

                val testClient = configureTestClient()

                val response = testClient.post("api/v1/veileder/opplysninger-om-arbeidssoeker") {
                    bearerAuth(mockOAuth2Server.issueAzureToken())
                    contentType(ContentType.Application.Json)
                    setBody(
                        OpplysningerOmArbeidssoekerRequest(
                            identitetsnummer = periode.identitetsnummer, periodeId = periode.id
                        )
                    )
                }

                response.status shouldBe HttpStatusCode.OK
                val opplysningerResponses = response.body<List<OpplysningerOmArbeidssoekerResponse>>()
                opplysningerResponses.size shouldBe 3
                opplysninger[0] shouldBeEqualTo opplysningerResponses[0]
                opplysninger[1] shouldBeEqualTo opplysningerResponses[1]
                opplysninger[2] shouldBeEqualTo opplysningerResponses[2]

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                verify { poaoTilgangHttpConsumerMock.evaluatePolicies(any<List<PolicyRequest>>()) }
            }
        }

        "/veileder/opplysninger-om-arbeidssoeker med siste-flagg should return 200 OK" {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns listOf(IdentInformasjon(TestData.fnr9, IdentGruppe.FOLKEREGISTERIDENT))
            every {
                poaoTilgangHttpConsumerMock.evaluatePolicies(any<List<PolicyRequest>>())
            } returns listOf(PolicyResult(UUID.randomUUID(), Decision.Permit))

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        opplysningerRoutes(authorizationService, periodeService, opplysningerService)
                    }
                }

                val periode = TestData.nyStartetPeriode(identitetsnummer = TestData.fnr9)
                val opplysninger = TestData.nyOpplysningerOmArbeidssoekerList(size = 3, periodeId = periode.id)
                periodeService.lagreAllePerioder(listOf(periode))
                opplysningerService.lagreAlleOpplysninger(opplysninger)

                val testClient = configureTestClient()

                val response = testClient.post("api/v1/veileder/opplysninger-om-arbeidssoeker?siste=true") {
                    bearerAuth(mockOAuth2Server.issueAzureToken())
                    contentType(ContentType.Application.Json)
                    setBody(
                        OpplysningerOmArbeidssoekerRequest(
                            identitetsnummer = periode.identitetsnummer, periodeId = periode.id
                        )
                    )
                }

                response.status shouldBe HttpStatusCode.OK
                val opplysningerResponses = response.body<List<OpplysningerOmArbeidssoekerResponse>>()
                opplysningerResponses.size shouldBe 1
                opplysninger[0] shouldBeEqualTo opplysningerResponses[0]

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                verify { poaoTilgangHttpConsumerMock.evaluatePolicies(any<List<PolicyRequest>>()) }
            }
        }

        "/veileder/opplysninger-om-arbeidssoeker should return 200 OK for identiteter" {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns listOf(IdentInformasjon(TestData.fnr10, IdentGruppe.FOLKEREGISTERIDENT))
            every {
                poaoTilgangHttpConsumerMock.evaluatePolicies(any<List<PolicyRequest>>())
            } returns listOf(PolicyResult(UUID.randomUUID(), Decision.Permit))

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        opplysningerRoutes(authorizationService, periodeService, opplysningerService)
                    }
                }

                val periode = TestData.nyStartetPeriode(identitetsnummer = TestData.fnr10)
                val opplysninger = TestData.nyOpplysningerOmArbeidssoekerList(size = 3, periodeId = periode.id)
                periodeService.lagreAllePerioder(listOf(periode))
                opplysningerService.lagreAlleOpplysninger(opplysninger)

                val testClient = configureTestClient()

                val response = testClient.post("api/v1/veileder/opplysninger-om-arbeidssoeker") {
                    bearerAuth(mockOAuth2Server.issueAzureToken())
                    contentType(ContentType.Application.Json)
                    setBody(
                        OpplysningerOmArbeidssoekerRequest(
                            identitetsnummer = periode.identitetsnummer
                        )
                    )
                }

                response.status shouldBe HttpStatusCode.OK
                val opplysningerResponses = response.body<List<OpplysningerOmArbeidssoekerResponse>>()
                opplysningerResponses.size shouldBe 3
                opplysninger[0] shouldBeEqualTo opplysningerResponses[0]
                opplysninger[1] shouldBeEqualTo opplysningerResponses[1]
                opplysninger[2] shouldBeEqualTo opplysningerResponses[2]

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                verify { poaoTilgangHttpConsumerMock.evaluatePolicies(any<List<PolicyRequest>>()) }
            }
        }

        "/veileder/opplysninger-om-arbeidssoeker should return 200 OK for identiteter med M2M token" {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns listOf(IdentInformasjon(TestData.fnr11, IdentGruppe.FOLKEREGISTERIDENT))

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        opplysningerRoutes(authorizationService, periodeService, opplysningerService)
                    }
                }

                val periode = TestData.nyStartetPeriode(identitetsnummer = TestData.fnr11)
                val opplysninger = TestData.nyOpplysningerOmArbeidssoekerList(size = 3, periodeId = periode.id)
                periodeService.lagreAllePerioder(listOf(periode))
                opplysningerService.lagreAlleOpplysninger(opplysninger)

                val testClient = configureTestClient()

                val response = testClient.post("api/v1/veileder/opplysninger-om-arbeidssoeker") {
                    bearerAuth(mockOAuth2Server.issueAzureM2MToken())
                    contentType(ContentType.Application.Json)
                    setBody(
                        OpplysningerOmArbeidssoekerRequest(
                            identitetsnummer = periode.identitetsnummer
                        )
                    )
                }

                response.status shouldBe HttpStatusCode.OK
                val opplysningerResponses = response.body<List<OpplysningerOmArbeidssoekerResponse>>()
                opplysningerResponses.size shouldBe 3
                opplysninger[0] shouldBeEqualTo opplysningerResponses[0]
                opplysninger[1] shouldBeEqualTo opplysningerResponses[1]
                opplysninger[2] shouldBeEqualTo opplysningerResponses[2]

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
            }
        }
    }
})
