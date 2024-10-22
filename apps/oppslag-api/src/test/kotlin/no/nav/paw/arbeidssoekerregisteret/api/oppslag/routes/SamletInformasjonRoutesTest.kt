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
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.SamletInformasjonRequest
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.SamletInformasjonResponse
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

class SamletInformasjonRoutesTest : FreeSpec({
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

            every {
                periodeRepository.finnPerioderForIdentiteter(any<List<Identitetsnummer>>())
            } returns TestData.nyPeriodeRowList()

            every {
                opplysningerRepository.finnOpplysningerForPeriodeId(any<UUID>())
            } returns TestData.nyOpplysningerRowList()

            every {
                opplysningerRepository.finnOpplysningerForIdentiteter(any<List<Identitetsnummer>>())
            } returns TestData.nyOpplysningerRowList()

            every {
                profileringRepository.finnProfileringerForPeriodeId(any<UUID>())
            } returns TestData.nyProfileringRowList()

            every {
                profileringRepository.finnProfileringerForIdentiteter(any<List<Identitetsnummer>>())
            } returns TestData.nyProfileringRowList()
        }

        "/samlet-informasjon should return 401 Unauthorized without token" {
            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        samletInformasjonRoutes(
                            authorizationService,
                            periodeService,
                            opplysningerService,
                            profileringService
                        )
                    }
                }

                val testClient = configureTestClient()

                val response = testClient.get("api/v1/samlet-informasjon")

                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        "/samlet-informasjon should return OK" {
            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        samletInformasjonRoutes(
                            authorizationService,
                            periodeService,
                            opplysningerService,
                            profileringService
                        )
                    }
                }

                val testClient = configureTestClient()

                val response = testClient.get("api/v1/samlet-informasjon") {
                    bearerAuth(mockOAuth2Server.issueTokenXToken())
                }

                response.status shouldBe HttpStatusCode.OK
                val samletInfo = response.body<SamletInformasjonResponse>()
                samletInfo.arbeidssoekerperioder.size shouldBe 3
                samletInfo.opplysningerOmArbeidssoeker.size shouldBe 3
                samletInfo.profilering.size shouldBe 3

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                verify { periodeRepository.finnPerioderForIdentiteter(any<List<Identitetsnummer>>()) }
                verify { opplysningerRepository.finnOpplysningerForIdentiteter(any<List<Identitetsnummer>>()) }
                verify { profileringRepository.finnProfileringerForIdentiteter(any<List<Identitetsnummer>>()) }
            }
        }

        "/samlet-informasjon med siste-flagg should return OK" {
            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        samletInformasjonRoutes(
                            authorizationService,
                            periodeService,
                            opplysningerService,
                            profileringService
                        )
                    }
                }

                val testClient = configureTestClient()

                val response = testClient.get("api/v1/samlet-informasjon?siste=true") {
                    bearerAuth(mockOAuth2Server.issueTokenXToken())
                }

                response.status shouldBe HttpStatusCode.OK
                val samletInfo = response.body<SamletInformasjonResponse>()
                samletInfo.arbeidssoekerperioder.size shouldBe 1
                samletInfo.opplysningerOmArbeidssoeker.size shouldBe 1
                samletInfo.profilering.size shouldBe 1

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                verify { periodeRepository.finnPerioderForIdentiteter(any<List<Identitetsnummer>>()) }
                verify { opplysningerRepository.finnOpplysningerForPeriodeId(any<UUID>()) }
                verify { profileringRepository.finnProfileringerForPeriodeId(any<UUID>()) }
            }
        }

        "/veileder/samlet-informasjon should return 403 uten POAO Tilgang" {
            every {
                poaoTilgangHttpClientMock.evaluatePolicies(any<List<PolicyRequest>>())
            } returns ApiResult.success(listOf(PolicyResult(UUID.randomUUID(), Decision.Deny("test", "test"))))

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        samletInformasjonRoutes(
                            authorizationService,
                            periodeService,
                            opplysningerService,
                            profileringService
                        )
                    }
                }

                val testClient = configureTestClient()

                val response = testClient.post("api/v1/veileder/samlet-informasjon") {
                    bearerAuth(mockOAuth2Server.issueAzureToken())
                    contentType(ContentType.Application.Json)
                    setBody(
                        SamletInformasjonRequest(
                            identitetsnummer = "12345678901"
                        )
                    )
                }

                response.status shouldBe HttpStatusCode.Forbidden

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                verify { poaoTilgangHttpClientMock.evaluatePolicies(any<List<PolicyRequest>>()) }
            }
        }

        "/veileder/samlet-informasjon should return OK" {
            every {
                poaoTilgangHttpClientMock.evaluatePolicies(any<List<PolicyRequest>>())
            } returns ApiResult.success(listOf(PolicyResult(UUID.randomUUID(), Decision.Permit)))

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        samletInformasjonRoutes(
                            authorizationService,
                            periodeService,
                            opplysningerService,
                            profileringService
                        )
                    }
                }

                val testClient = configureTestClient()

                val response = testClient.post("api/v1/veileder/samlet-informasjon") {
                    bearerAuth(mockOAuth2Server.issueAzureToken())
                    contentType(ContentType.Application.Json)
                    setBody(
                        SamletInformasjonRequest(
                            identitetsnummer = "12345678901"
                        )
                    )
                }

                response.status shouldBe HttpStatusCode.OK
                val samletInfo = response.body<SamletInformasjonResponse>()
                samletInfo.arbeidssoekerperioder.size shouldBe 3
                samletInfo.opplysningerOmArbeidssoeker.size shouldBe 3
                samletInfo.profilering.size shouldBe 3

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                verify { poaoTilgangHttpClientMock.evaluatePolicies(any<List<PolicyRequest>>()) }
                verify { periodeRepository.finnPerioderForIdentiteter(any<List<Identitetsnummer>>()) }
                verify { opplysningerRepository.finnOpplysningerForIdentiteter(any<List<Identitetsnummer>>()) }
                verify { profileringRepository.finnProfileringerForIdentiteter(any<List<Identitetsnummer>>()) }
            }
        }

        "/veileder/samlet-informasjon med siste-flagg should return OK" {
            every {
                poaoTilgangHttpClientMock.evaluatePolicies(any<List<PolicyRequest>>())
            } returns ApiResult.success(listOf(PolicyResult(UUID.randomUUID(), Decision.Permit)))

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        samletInformasjonRoutes(
                            authorizationService,
                            periodeService,
                            opplysningerService,
                            profileringService
                        )
                    }
                }

                val testClient = configureTestClient()

                val response = testClient.post("api/v1/veileder/samlet-informasjon?siste=true") {
                    bearerAuth(mockOAuth2Server.issueAzureToken())
                    contentType(ContentType.Application.Json)
                    setBody(
                        SamletInformasjonRequest(
                            identitetsnummer = "12345678901"
                        )
                    )
                }

                response.status shouldBe HttpStatusCode.OK
                val samletInfo = response.body<SamletInformasjonResponse>()
                samletInfo.arbeidssoekerperioder.size shouldBe 1
                samletInfo.opplysningerOmArbeidssoeker.size shouldBe 1
                samletInfo.profilering.size shouldBe 1

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                verify { poaoTilgangHttpClientMock.evaluatePolicies(any<List<PolicyRequest>>()) }
                verify { periodeRepository.finnPerioderForIdentiteter(any<List<Identitetsnummer>>()) }
                verify { opplysningerRepository.finnOpplysningerForPeriodeId(any<UUID>()) }
                verify { profileringRepository.finnProfileringerForPeriodeId(any<UUID>()) }
            }
        }
    }
})
