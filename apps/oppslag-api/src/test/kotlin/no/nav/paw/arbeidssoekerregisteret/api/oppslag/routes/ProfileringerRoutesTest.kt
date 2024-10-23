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
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ProfileringRequest
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ProfileringResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.configureHTTP
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.configureSerialization
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.ApplicationTestContext
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.TestData
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.issueAzureToken
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.issueTokenXToken
import no.nav.paw.pdl.graphql.generated.enums.IdentGruppe
import no.nav.paw.pdl.graphql.generated.hentidenter.IdentInformasjon
import no.nav.poao_tilgang.client.Decision
import no.nav.poao_tilgang.client.PolicyRequest
import no.nav.poao_tilgang.client.PolicyResult
import no.nav.poao_tilgang.client.api.ApiResult
import java.util.*

class ProfileringerRoutesTest : FreeSpec({
    with(ApplicationTestContext.withMockDataAccess()) {

        beforeSpec {
            mockOAuth2Server.start()
        }

        afterSpec {
            mockOAuth2Server.shutdown()
            confirmVerified(
                pdlHttpConsumerMock,
                poaoTilgangHttpClientMock,
                periodeRepository,
                opplysningerRepository,
                profileringRepository,
                bekreftelseRepository
            )
        }

        beforeTest {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns listOf(IdentInformasjon(TestData.fnr1, IdentGruppe.FOLKEREGISTERIDENT))
        }

        "/profilering/{periodeId} should return 401 Unauthorized without token" {
            every {
                profileringRepository.finnProfileringerForIdentiteter(any<List<Identitetsnummer>>())
            } returns emptyList()

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        profileringRoutes(authorizationService, periodeService, profileringService)
                    }
                }

                val testClient = configureTestClient()

                val noTokenResponse = testClient.get("api/v1/profilering/${TestData.periodeId1}")

                noTokenResponse.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        "/profilering/{periodeId} should return OK" {
            val periode = TestData.nyStartetPeriodeRow()
            val profileringer = TestData.nyProfileringRowList(size = 3, periodeId = periode.periodeId)
            every {
                periodeRepository.hentPeriodeForId(any<UUID>())
            } returns periode
            every {
                profileringRepository.finnProfileringerForPeriodeId(any<UUID>())
            } returns profileringer

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        profileringRoutes(authorizationService, periodeService, profileringService)
                    }
                }

                val testClient = configureTestClient()

                val response = testClient.get("api/v1/profilering/${periode.periodeId}") {
                    bearerAuth(mockOAuth2Server.issueTokenXToken())
                }

                response.status shouldBe HttpStatusCode.OK
                val profileringResponses = response.body<List<ProfileringResponse>>()
                profileringResponses.size shouldBe 3

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                verify { periodeRepository.hentPeriodeForId(any<UUID>()) }
                verify { profileringRepository.finnProfileringerForPeriodeId(any<UUID>()) }
            }
        }

        "/profilering should return OK" {
            every {
                profileringRepository.finnProfileringerForIdentiteter(any<List<Identitetsnummer>>())
            } returns TestData.nyProfileringRowList(size = 3)

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        profileringRoutes(authorizationService, periodeService, profileringService)
                    }
                }

                val testClient = configureTestClient()

                val response = testClient.get("api/v1/profilering") {
                    bearerAuth(mockOAuth2Server.issueTokenXToken())
                }

                response.status shouldBe HttpStatusCode.OK
                val profileringer = response.body<List<ProfileringResponse>>()
                profileringer.size shouldBe 3

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                verify { profileringRepository.finnProfileringerForIdentiteter(any<List<Identitetsnummer>>()) }
            }
        }

        "/profilering med siste-flagg should return OK" {
            every {
                profileringRepository.finnProfileringerForIdentiteter(any<List<Identitetsnummer>>())
            } returns TestData.nyProfileringRowList(size = 3, periodeId = TestData.periodeId3)

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        profileringRoutes(authorizationService, periodeService, profileringService)
                    }
                }

                val testClient = configureTestClient()

                val response = testClient.get("api/v1/profilering?siste=true") {
                    bearerAuth(mockOAuth2Server.issueTokenXToken())
                }

                response.status shouldBe HttpStatusCode.OK
                val profileringer = response.body<List<ProfileringResponse>>()
                profileringer.size shouldBe 1
                profileringer[0].periodeId shouldBe TestData.periodeId3

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                verify { profileringRepository.finnProfileringerForIdentiteter(any<List<Identitetsnummer>>()) }
            }
        }

        "/veileder/profilering should return 403 Forbidden uten POAO Tilgang" {
            every {
                poaoTilgangHttpClientMock.evaluatePolicies(any<List<PolicyRequest>>())
            } returns ApiResult.success(listOf(PolicyResult(UUID.randomUUID(), Decision.Deny("test", "test"))))
            every {
                periodeRepository.hentPeriodeForId(any<UUID>())
            } returns TestData.nyStartetPeriodeRow()
            every {
                profileringRepository.finnProfileringerForIdentiteter(any<List<Identitetsnummer>>())
            } returns TestData.nyProfileringRowList()

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        profileringRoutes(authorizationService, periodeService, profileringService)
                    }
                }

                val testClient = configureTestClient()

                val response = testClient.post("api/v1/veileder/profilering") {
                    bearerAuth(mockOAuth2Server.issueAzureToken())
                    contentType(ContentType.Application.Json)
                    setBody(
                        ProfileringRequest(
                            identitetsnummer = TestData.fnr1,
                            periodeId = TestData.periodeId1
                        )
                    )
                }

                response.status shouldBe HttpStatusCode.Forbidden

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                verify { poaoTilgangHttpClientMock.evaluatePolicies(any<List<PolicyRequest>>()) }
                verify { periodeRepository.hentPeriodeForId(any<UUID>()) }
                verify { profileringRepository.finnProfileringerForIdentiteter(any<List<Identitetsnummer>>()) }
            }
        }

        "/veileder/profilering should return 200 OK" {
            every {
                poaoTilgangHttpClientMock.evaluatePolicies(any<List<PolicyRequest>>())
            } returns ApiResult.success(listOf(PolicyResult(UUID.randomUUID(), Decision.Permit)))
            every {
                periodeRepository.hentPeriodeForId(any<UUID>())
            } returns TestData.nyStartetPeriodeRow()
            every {
                profileringRepository.finnProfileringerForPeriodeId(any<UUID>())
            } returns TestData.nyProfileringRowList(size = 3)

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        profileringRoutes(authorizationService, periodeService, profileringService)
                    }
                }

                val testClient = configureTestClient()

                val response = testClient.post("api/v1/veileder/profilering") {
                    bearerAuth(mockOAuth2Server.issueAzureToken())
                    contentType(ContentType.Application.Json)
                    setBody(
                        ProfileringRequest(
                            identitetsnummer = TestData.fnr1,
                            periodeId = TestData.periodeId1
                        )
                    )
                }

                response.status shouldBe HttpStatusCode.OK
                val profileringer = response.body<List<ProfileringResponse>>()
                profileringer.size shouldBe 3

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                verify { poaoTilgangHttpClientMock.evaluatePolicies(any<List<PolicyRequest>>()) }
                verify { periodeRepository.hentPeriodeForId(any<UUID>()) }
                verify { profileringRepository.finnProfileringerForPeriodeId(any<UUID>()) }
            }
        }

        "/veileder/profilering med siste-flagg should return 200 OK" {
            every {
                poaoTilgangHttpClientMock.evaluatePolicies(any<List<PolicyRequest>>())
            } returns ApiResult.success(listOf(PolicyResult(UUID.randomUUID(), Decision.Permit)))
            every {
                periodeRepository.hentPeriodeForId(any<UUID>())
            } returns TestData.nyStartetPeriodeRow()
            every {
                profileringRepository.finnProfileringerForPeriodeId(any<UUID>())
            } returns TestData.nyProfileringRowList(size = 3, periodeId = TestData.periodeId3)

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        profileringRoutes(authorizationService, periodeService, profileringService)
                    }
                }

                val testClient = configureTestClient()

                val response = testClient.post("api/v1/veileder/profilering?siste=true") {
                    bearerAuth(mockOAuth2Server.issueAzureToken())
                    contentType(ContentType.Application.Json)
                    setBody(
                        ProfileringRequest(
                            identitetsnummer = TestData.fnr1,
                            periodeId = TestData.periodeId1
                        )
                    )
                }

                response.status shouldBe HttpStatusCode.OK
                val profileringer = response.body<List<ProfileringResponse>>()
                profileringer.size shouldBe 1
                profileringer[0].periodeId shouldBe TestData.periodeId3

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                verify { poaoTilgangHttpClientMock.evaluatePolicies(any<List<PolicyRequest>>()) }
                verify { periodeRepository.hentPeriodeForId(any<UUID>()) }
                verify { profileringRepository.finnProfileringerForPeriodeId(any<UUID>()) }
            }
        }
    }
})
