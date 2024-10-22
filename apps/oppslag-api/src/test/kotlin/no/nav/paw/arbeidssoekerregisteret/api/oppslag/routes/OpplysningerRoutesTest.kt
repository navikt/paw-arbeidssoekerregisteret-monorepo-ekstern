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
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.OpplysningerOmArbeidssoekerRequest
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.OpplysningerOmArbeidssoekerResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.configureHTTP
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.configureSerialization
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.ApplicationTestContext
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.TestData
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

class OpplysningerRoutesTest : FreeSpec({
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
            } returns listOf(IdentInformasjon(TestData.fnr1, IdentGruppe.FOLKEREGISTERIDENT))
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

                val noTokenResponse = testClient
                    .get("api/v1/opplysninger-om-arbeidssoeker")

                noTokenResponse.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        "/opplysninger-om-arbeidssoeker should return OK" {
            every {
                opplysningerRepositoryMock.finnOpplysningerForIdentiteter(any<List<Identitetsnummer>>())
            } returns TestData.nyOpplysningerRowList()

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

                val response = testClient.get("api/v1/opplysninger-om-arbeidssoeker") {
                    bearerAuth(mockOAuth2Server.issueTokenXToken())
                }

                response.status shouldBe HttpStatusCode.OK
                val opplysninger = response.body<List<OpplysningerOmArbeidssoekerResponse>>()
                opplysninger.size shouldBe 3

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                verify { opplysningerRepositoryMock.finnOpplysningerForIdentiteter(any<List<Identitetsnummer>>()) }
            }
        }

        "/opplysninger-om-arbeidssoeker med siste-flagg should return OK" {
            every {
                opplysningerRepositoryMock.finnOpplysningerForIdentiteter(any<List<Identitetsnummer>>())
            } returns TestData.nyOpplysningerRowList()

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

                val response = testClient.get("api/v1/opplysninger-om-arbeidssoeker?siste=true") {
                    bearerAuth(mockOAuth2Server.issueTokenXToken())
                }

                response.status shouldBe HttpStatusCode.OK
                val opplysninger = response.body<List<OpplysningerOmArbeidssoekerResponse>>()
                opplysninger.size shouldBe 1
                opplysninger[0].periodeId shouldBe TestData.periodeId1

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                verify { opplysningerRepositoryMock.finnOpplysningerForIdentiteter(any<List<Identitetsnummer>>()) }
            }
        }

        "/opplysninger-om-arbeidssoeker/{periodeId} should return 400 BadRequest if periode does not exist for periodeId" {
            every {
                periodeRepositoryMock.hentPeriodeForId(any<UUID>())
            } returns null

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
                    bearerAuth(mockOAuth2Server.issueTokenXToken())
                }

                response.status shouldBe HttpStatusCode.BadRequest

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                verify { periodeRepositoryMock.hentPeriodeForId(any<UUID>()) }
            }
        }

        "/opplysninger-om-arbeidssoeker/{periodeId} should return 403 Forbidden if periodeId does not exist for user" {
            every {
                periodeRepositoryMock.hentPeriodeForId(any<UUID>())
            } returns TestData.nyStartetPeriodeRow(identitetsnummer = TestData.fnr2)

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
                    bearerAuth(mockOAuth2Server.issueTokenXToken())
                }

                response.status shouldBe HttpStatusCode.Forbidden

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                verify { periodeRepositoryMock.hentPeriodeForId(any<UUID>()) }
            }
        }

        "/opplysninger-om-arbeidssoeker/{periodeId} should return OK" {
            every {
                periodeRepositoryMock.hentPeriodeForId(any<UUID>())
            } returns TestData.nyStartetPeriodeRow()

            every {
                opplysningerRepositoryMock.finnOpplysningerForPeriodeId(any<UUID>())
            } returns TestData.nyOpplysningerRowList()

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

                val response = testClient
                    .get("api/v1/opplysninger-om-arbeidssoeker/${TestData.periodeId1}") {
                        bearerAuth(mockOAuth2Server.issueTokenXToken())
                    }

                response.status shouldBe HttpStatusCode.OK

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                verify { periodeRepositoryMock.hentPeriodeForId(any<UUID>()) }
                verify { opplysningerRepositoryMock.finnOpplysningerForPeriodeId(any<UUID>()) }
            }
        }

        "/veileder/opplysninger-om-arbeidssoeker should return 403 Forbidden uten POAO Tilgang" {
            every {
                poaoTilgangHttpClientMock.evaluatePolicies(any<List<PolicyRequest>>())
            } returns ApiResult.success(
                listOf(
                    PolicyResult(UUID.randomUUID(), Decision.Deny("test", "test")),
                    PolicyResult(UUID.randomUUID(), Decision.Permit)
                )
            )

            every {
                opplysningerRepositoryMock.finnOpplysningerForIdentiteter(any<List<Identitetsnummer>>())
            } returns TestData.nyOpplysningerRowList()

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

                val response = testClient
                    .post("api/v1/veileder/opplysninger-om-arbeidssoeker") {
                        bearerAuth(mockOAuth2Server.issueAzureToken())
                        contentType(ContentType.Application.Json)
                        setBody(
                            OpplysningerOmArbeidssoekerRequest(
                                identitetsnummer = TestData.fnr1,
                                periodeId = TestData.periodeId1
                            )
                        )
                    }

                response.status shouldBe HttpStatusCode.Forbidden

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                verify { poaoTilgangHttpClientMock.evaluatePolicies(any<List<PolicyRequest>>()) }
            }
        }

        "/veileder/opplysninger-om-arbeidssoeker should return 403 Forbidden når periode ikke tilhører sluttbruker" {
            every {
                poaoTilgangHttpClientMock.evaluatePolicies(any<List<PolicyRequest>>())
            } returns ApiResult.success(listOf(PolicyResult(UUID.randomUUID(), Decision.Permit)))

            every {
                periodeRepositoryMock.hentPeriodeForId(any<UUID>())
            } returns TestData.nyStartetPeriodeRow(identitetsnummer = TestData.fnr2)

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

                val response = testClient
                    .post("api/v1/veileder/opplysninger-om-arbeidssoeker") {
                        bearerAuth(mockOAuth2Server.issueAzureToken())
                        contentType(ContentType.Application.Json)
                        setBody(
                            OpplysningerOmArbeidssoekerRequest(
                                identitetsnummer = TestData.fnr1,
                                periodeId = TestData.periodeId1
                            )
                        )
                    }

                response.status shouldBe HttpStatusCode.Forbidden

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                verify { poaoTilgangHttpClientMock.evaluatePolicies(any<List<PolicyRequest>>()) }
                verify { periodeRepositoryMock.hentPeriodeForId(any<UUID>()) }
            }
        }

        "/veileder/opplysninger-om-arbeidssoeker should return 200 OK for periodeId med POAO Tilgang" {
            every {
                poaoTilgangHttpClientMock.evaluatePolicies(any<List<PolicyRequest>>())
            } returns ApiResult.success(
                listOf(
                    PolicyResult(UUID.randomUUID(), Decision.Permit),
                    PolicyResult(UUID.randomUUID(), Decision.Permit)
                )
            )

            every {
                periodeRepositoryMock.hentPeriodeForId(any<UUID>())
            } returns TestData.nyStartetPeriodeRow()

            every {
                opplysningerRepositoryMock.finnOpplysningerForPeriodeId(any<UUID>())
            } returns TestData.nyOpplysningerRowList()

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

                val response = testClient
                    .post("api/v1/veileder/opplysninger-om-arbeidssoeker") {
                        bearerAuth(mockOAuth2Server.issueAzureToken())
                        contentType(ContentType.Application.Json)
                        setBody(
                            OpplysningerOmArbeidssoekerRequest(
                                identitetsnummer = TestData.fnr1,
                                periodeId = TestData.periodeId1
                            )
                        )
                    }

                response.status shouldBe HttpStatusCode.OK
                val opplysninger = response.body<List<OpplysningerOmArbeidssoekerResponse>>()
                opplysninger.size shouldBe 3

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                verify { poaoTilgangHttpClientMock.evaluatePolicies(any<List<PolicyRequest>>()) }
                verify { periodeRepositoryMock.hentPeriodeForId(any<UUID>()) }
                verify { opplysningerRepositoryMock.finnOpplysningerForPeriodeId(any<UUID>()) }
            }
        }

        "/veileder/opplysninger-om-arbeidssoeker med siste-flagg should return 200 OK for periodeId med POAO Tilgang" {
            every {
                poaoTilgangHttpClientMock.evaluatePolicies(any<List<PolicyRequest>>())
            } returns ApiResult.success(listOf(PolicyResult(UUID.randomUUID(), Decision.Permit)))

            every {
                periodeRepositoryMock.hentPeriodeForId(any<UUID>())
            } returns TestData.nyStartetPeriodeRow()

            every {
                opplysningerRepositoryMock.finnOpplysningerForPeriodeId(any<UUID>())
            } returns TestData.nyOpplysningerRowList()

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

                val response = testClient
                    .post("api/v1/veileder/opplysninger-om-arbeidssoeker?siste=true") {
                        bearerAuth(mockOAuth2Server.issueAzureToken())
                        contentType(ContentType.Application.Json)
                        setBody(
                            OpplysningerOmArbeidssoekerRequest(
                                identitetsnummer = TestData.fnr1,
                                periodeId = TestData.periodeId1
                            )
                        )
                    }

                response.status shouldBe HttpStatusCode.OK
                val opplysninger = response.body<List<OpplysningerOmArbeidssoekerResponse>>()
                opplysninger.size shouldBe 1
                opplysninger[0].periodeId shouldBe TestData.periodeId1

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                verify { poaoTilgangHttpClientMock.evaluatePolicies(any<List<PolicyRequest>>()) }
                verify { periodeRepositoryMock.hentPeriodeForId(any<UUID>()) }
                verify { opplysningerRepositoryMock.finnOpplysningerForPeriodeId(any<UUID>()) }
            }
        }

        "/veileder/opplysninger-om-arbeidssoeker should return 200 OK for identiteter med POAO Tilgang" {
            every {
                poaoTilgangHttpClientMock.evaluatePolicies(any<List<PolicyRequest>>())
            } returns ApiResult.success(listOf(PolicyResult(UUID.randomUUID(), Decision.Permit)))

            every {
                opplysningerRepositoryMock.finnOpplysningerForIdentiteter(any<List<Identitetsnummer>>())
            } returns TestData.nyOpplysningerRowList()

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

                val response = testClient
                    .post("api/v1/veileder/opplysninger-om-arbeidssoeker") {
                        bearerAuth(mockOAuth2Server.issueAzureToken())
                        contentType(ContentType.Application.Json)
                        setBody(
                            OpplysningerOmArbeidssoekerRequest(
                                identitetsnummer = TestData.fnr1
                            )
                        )
                    }

                response.status shouldBe HttpStatusCode.OK
                val opplysninger = response.body<List<OpplysningerOmArbeidssoekerResponse>>()
                opplysninger.size shouldBe 3

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                verify { poaoTilgangHttpClientMock.evaluatePolicies(any<List<PolicyRequest>>()) }
                verify { opplysningerRepositoryMock.finnOpplysningerForIdentiteter(any<List<Identitetsnummer>>()) }
            }
        }

        "/veileder/opplysninger-om-arbeidssoeker should return 200 OK for identiteter med M2M token" {
            every {
                opplysningerRepositoryMock.finnOpplysningerForIdentiteter(any<List<Identitetsnummer>>())
            } returns TestData.nyOpplysningerRowList()

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

                val response = testClient
                    .post("api/v1/veileder/opplysninger-om-arbeidssoeker") {
                        bearerAuth(mockOAuth2Server.issueAzureM2MToken())
                        contentType(ContentType.Application.Json)
                        setBody(
                            OpplysningerOmArbeidssoekerRequest(
                                identitetsnummer = TestData.fnr1
                            )
                        )
                    }

                response.status shouldBe HttpStatusCode.OK
                val opplysninger = response.body<List<OpplysningerOmArbeidssoekerResponse>>()
                opplysninger.size shouldBe 3

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                verify { opplysningerRepositoryMock.finnOpplysningerForIdentiteter(any<List<Identitetsnummer>>()) }
            }
        }
    }
})
