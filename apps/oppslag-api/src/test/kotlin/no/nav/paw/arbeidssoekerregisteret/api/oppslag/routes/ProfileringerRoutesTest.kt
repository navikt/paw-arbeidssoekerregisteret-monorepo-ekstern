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
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.shouldBeEqualTo
import no.nav.paw.pdl.graphql.generated.enums.IdentGruppe
import no.nav.paw.pdl.graphql.generated.hentidenter.IdentInformasjon
import no.nav.poao_tilgang.client.Decision
import no.nav.poao_tilgang.client.PolicyRequest
import no.nav.poao_tilgang.client.PolicyResult
import no.nav.poao_tilgang.client.api.ApiResult
import java.util.*

class ProfileringerRoutesTest : FreeSpec({
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

        "/profilering/{periodeId} should return 401 Unauthorized without token" {
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
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns listOf(IdentInformasjon(TestData.fnr1, IdentGruppe.FOLKEREGISTERIDENT))

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        profileringRoutes(authorizationService, periodeService, profileringService)
                    }
                }

                val periode = TestData.nyStartetPeriode()
                val profileringer = TestData.nyProfileringList(size = 3, periodeId = periode.id)
                periodeService.lagreAllePerioder(listOf(periode).asSequence())
                profileringService.lagreAlleProfileringer(profileringer.asSequence())

                val testClient = configureTestClient()

                val response = testClient.get("api/v1/profilering/${periode.id}") {
                    bearerAuth(mockOAuth2Server.issueTokenXToken())
                }

                response.status shouldBe HttpStatusCode.OK
                val profileringResponses = response.body<List<ProfileringResponse>>()
                profileringResponses.size shouldBe 3
                profileringer[0] shouldBeEqualTo profileringResponses[0]
                profileringer[1] shouldBeEqualTo profileringResponses[1]
                profileringer[2] shouldBeEqualTo profileringResponses[2]

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
            }
        }

        "/profilering should return OK" {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns listOf(IdentInformasjon(TestData.fnr3, IdentGruppe.FOLKEREGISTERIDENT))

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        profileringRoutes(authorizationService, periodeService, profileringService)
                    }
                }

                val periode = TestData.nyStartetPeriode(identitetsnummer = TestData.fnr3)
                val profileringer = TestData.nyProfileringList(size = 3, periodeId = periode.id)
                periodeService.lagreAllePerioder(listOf(periode).asSequence())
                profileringService.lagreAlleProfileringer(profileringer.asSequence())

                val testClient = configureTestClient()

                val response = testClient.get("api/v1/profilering") {
                    bearerAuth(mockOAuth2Server.issueTokenXToken(pid = TestData.fnr3))
                }

                response.status shouldBe HttpStatusCode.OK
                val profileringResponses = response.body<List<ProfileringResponse>>()
                profileringResponses.size shouldBe 3
                profileringer[0] shouldBeEqualTo profileringResponses[0]
                profileringer[1] shouldBeEqualTo profileringResponses[1]
                profileringer[2] shouldBeEqualTo profileringResponses[2]

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
            }
        }

        "/profilering med siste-flagg should return OK" {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns listOf(IdentInformasjon(TestData.fnr1, IdentGruppe.FOLKEREGISTERIDENT))

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        profileringRoutes(authorizationService, periodeService, profileringService)
                    }
                }

                val periode = TestData.nyStartetPeriode()
                val profileringer = TestData.nyProfileringList(size = 3, periodeId = periode.id)
                periodeService.lagreAllePerioder(listOf(periode).asSequence())
                profileringService.lagreAlleProfileringer(profileringer.asSequence())

                val testClient = configureTestClient()

                val response = testClient.get("api/v1/profilering?siste=true") {
                    bearerAuth(mockOAuth2Server.issueTokenXToken())
                }

                response.status shouldBe HttpStatusCode.OK
                val profileringResponses = response.body<List<ProfileringResponse>>()
                profileringResponses.size shouldBe 1
                profileringer[0] shouldBeEqualTo profileringResponses[0]

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
            }
        }

        "/veileder/profilering should return 403 Forbidden uten POAO Tilgang" {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns listOf(IdentInformasjon(TestData.fnr1, IdentGruppe.FOLKEREGISTERIDENT))
            every {
                poaoTilgangHttpClientMock.evaluatePolicies(any<List<PolicyRequest>>())
            } returns ApiResult.success(listOf(PolicyResult(UUID.randomUUID(), Decision.Deny("test", "test"))))

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
                            periodeId = TestData.periodeId3
                        )
                    )
                }

                response.status shouldBe HttpStatusCode.Forbidden

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                verify { poaoTilgangHttpClientMock.evaluatePolicies(any<List<PolicyRequest>>()) }
            }
        }

        "/veileder/profilering should return 200 OK" {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns listOf(IdentInformasjon(TestData.fnr4, IdentGruppe.FOLKEREGISTERIDENT))
            every {
                poaoTilgangHttpClientMock.evaluatePolicies(any<List<PolicyRequest>>())
            } returns ApiResult.success(listOf(PolicyResult(UUID.randomUUID(), Decision.Permit)))

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        profileringRoutes(authorizationService, periodeService, profileringService)
                    }
                }

                val periode = TestData.nyStartetPeriode(identitetsnummer = TestData.fnr4)
                val profileringer = TestData.nyProfileringList(size = 3, periodeId = periode.id)
                periodeService.lagreAllePerioder(listOf(periode).asSequence())
                profileringService.lagreAlleProfileringer(profileringer.asSequence())

                val testClient = configureTestClient()

                val response = testClient.post("api/v1/veileder/profilering") {
                    bearerAuth(mockOAuth2Server.issueAzureToken())
                    contentType(ContentType.Application.Json)
                    setBody(
                        ProfileringRequest(
                            identitetsnummer = periode.identitetsnummer,
                            periodeId = periode.id
                        )
                    )
                }

                response.status shouldBe HttpStatusCode.OK
                val profileringResponses = response.body<List<ProfileringResponse>>()
                profileringResponses.size shouldBe 3
                profileringer[0] shouldBeEqualTo profileringResponses[0]
                profileringer[1] shouldBeEqualTo profileringResponses[1]
                profileringer[2] shouldBeEqualTo profileringResponses[2]

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                verify { poaoTilgangHttpClientMock.evaluatePolicies(any<List<PolicyRequest>>()) }
            }
        }

        "/veileder/profilering med siste-flagg should return 200 OK" {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns listOf(IdentInformasjon(TestData.fnr5, IdentGruppe.FOLKEREGISTERIDENT))
            every {
                poaoTilgangHttpClientMock.evaluatePolicies(any<List<PolicyRequest>>())
            } returns ApiResult.success(listOf(PolicyResult(UUID.randomUUID(), Decision.Permit)))

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        profileringRoutes(authorizationService, periodeService, profileringService)
                    }
                }

                val periode = TestData.nyStartetPeriode(identitetsnummer = TestData.fnr5)
                val profileringer = TestData.nyProfileringList(size = 3, periodeId = periode.id)
                periodeService.lagreAllePerioder(listOf(periode).asSequence())
                profileringService.lagreAlleProfileringer(profileringer.asSequence())

                val testClient = configureTestClient()

                val response = testClient.post("api/v1/veileder/profilering?siste=true") {
                    bearerAuth(mockOAuth2Server.issueAzureToken())
                    contentType(ContentType.Application.Json)
                    setBody(
                        ProfileringRequest(
                            identitetsnummer = periode.identitetsnummer,
                            periodeId = periode.id
                        )
                    )
                }

                response.status shouldBe HttpStatusCode.OK
                val profileringResponses = response.body<List<ProfileringResponse>>()
                profileringResponses.size shouldBe 1
                profileringer[0] shouldBeEqualTo profileringResponses[0]

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                verify { poaoTilgangHttpClientMock.evaluatePolicies(any<List<PolicyRequest>>()) }
            }
        }
    }
})
