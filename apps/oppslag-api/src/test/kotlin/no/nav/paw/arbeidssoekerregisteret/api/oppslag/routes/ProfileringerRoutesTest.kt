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
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ProfileringRequest
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ProfileringResponse
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

class ProfileringerRoutesTest : FreeSpec({
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

        "/profilering/{periodeId} should return 403 Forbidden without token" {
            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        profileringRoutes(authorizationService, profileringService)
                    }
                }

                val testClient = configureTestClient()
                val response = testClient.get("api/v1/profilering/${TestData.periodeId1}")
                response.status shouldBe HttpStatusCode.Forbidden
            }
        }

        "/profilering/{periodeId} should return 403 Forbidden with Azure token" {
            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        profileringRoutes(authorizationService, profileringService)
                    }
                }

                val testClient = configureTestClient()
                val response = testClient.get("api/v1/profilering/${TestData.periodeId1}") {
                    bearerAuth(mockOAuth2Server.issueAzureToken())
                }
                response.status shouldBe HttpStatusCode.Forbidden
            }
        }

        "/profilering/{periodeId} should return 403 Forbidden with Azure M2M token" {
            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        profileringRoutes(authorizationService, profileringService)
                    }
                }

                val testClient = configureTestClient()
                val response = testClient.get("api/v1/profilering/${TestData.periodeId1}") {
                    bearerAuth(mockOAuth2Server.issueAzureM2MToken())
                }
                response.status shouldBe HttpStatusCode.Forbidden
            }
        }

        "/profilering/{periodeId} should return 400 Bad Request with unknown periode" {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns listOf(IdentInformasjon(TestData.fnr1, IdentGruppe.FOLKEREGISTERIDENT))

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        profileringRoutes(authorizationService, profileringService)
                    }
                }

                val testClient = configureTestClient()
                val response = testClient.get("api/v1/profilering/${TestData.periodeId1}") {
                    bearerAuth(mockOAuth2Server.issueTokenXToken())
                }

                response.status shouldBe HttpStatusCode.BadRequest

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
            }
        }

        "/profilering/{periodeId} should return 200 OK" {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns listOf(IdentInformasjon(TestData.fnr1, IdentGruppe.FOLKEREGISTERIDENT))

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        profileringRoutes(authorizationService, profileringService)
                    }
                }

                val periode = TestData.nyStartetPeriode(identitetsnummer = TestData.fnr1)
                val profileringer = TestData.nyProfileringList(size = 3, periodeId = periode.id)
                periodeService.lagrePeriode(periode)
                profileringer.forEach(profileringService::lagreProfilering)

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
            } returns listOf(IdentInformasjon(TestData.fnr2, IdentGruppe.FOLKEREGISTERIDENT))

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        profileringRoutes(authorizationService, profileringService)
                    }
                }

                val periode = TestData.nyStartetPeriode(identitetsnummer = TestData.fnr2)
                val profileringer = TestData.nyProfileringList(size = 3, periodeId = periode.id)
                periodeService.lagrePeriode(periode)
                profileringer.forEach(profileringService::lagreProfilering)

                val testClient = configureTestClient()
                val response = testClient.get("api/v1/profilering") {
                    bearerAuth(mockOAuth2Server.issueTokenXToken(pid = TestData.fnr2))
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
            } returns listOf(IdentInformasjon(TestData.fnr3, IdentGruppe.FOLKEREGISTERIDENT))

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        profileringRoutes(authorizationService, profileringService)
                    }
                }

                val periode = TestData.nyStartetPeriode(identitetsnummer = TestData.fnr3)
                val profileringer = TestData.nyProfileringList(size = 3, periodeId = periode.id)
                periodeService.lagrePeriode(periode)
                profileringer.forEach(profileringService::lagreProfilering)

                val testClient = configureTestClient()
                val response = testClient.get("api/v1/profilering?siste=true") {
                    bearerAuth(mockOAuth2Server.issueTokenXToken(pid = TestData.fnr3))
                }

                response.status shouldBe HttpStatusCode.OK
                val profileringResponses = response.body<List<ProfileringResponse>>()
                profileringResponses.size shouldBe 1
                profileringer[0] shouldBeEqualTo profileringResponses[0]

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
            }
        }

        "/veileder/profilering should return 403 Forbidden with TokenX token" {
            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        profileringRoutes(authorizationService, profileringService)
                    }
                }

                val testClient = configureTestClient()
                val response = testClient.post("api/v1/veileder/profilering") {
                    bearerAuth(mockOAuth2Server.issueTokenXToken())
                    contentType(ContentType.Application.Json)
                    setBody(
                        ProfileringRequest(
                            identitetsnummer = TestData.fnr4,
                            periodeId = TestData.periodeId4
                        )
                    )
                }

                response.status shouldBe HttpStatusCode.Forbidden
            }
        }

        "/veileder/profilering should return 403 Forbidden uten tilgang fra Tilgangskontroll" {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns listOf(IdentInformasjon(TestData.fnr5, IdentGruppe.FOLKEREGISTERIDENT))
            coEvery {
                tilgangskontrollClientMock.harAnsattTilgangTilPerson(any(), any(), any())
            } returns Data(false)

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        profileringRoutes(authorizationService, profileringService)
                    }
                }

                val testClient = configureTestClient()
                val response = testClient.post("api/v1/veileder/profilering") {
                    bearerAuth(mockOAuth2Server.issueAzureToken())
                    contentType(ContentType.Application.Json)
                    setBody(
                        ProfileringRequest(
                            identitetsnummer = TestData.fnr5,
                            periodeId = TestData.periodeId4
                        )
                    )
                }

                response.status shouldBe HttpStatusCode.Forbidden

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                coVerify { tilgangskontrollClientMock.harAnsattTilgangTilPerson(any(), any(), any()) }
            }
        }

        "/veileder/profilering should return 200 OK" {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns listOf(IdentInformasjon(TestData.fnr6, IdentGruppe.FOLKEREGISTERIDENT))
            coEvery {
                tilgangskontrollClientMock.harAnsattTilgangTilPerson(any(), any(), any())
            } returns Data(true)

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        profileringRoutes(authorizationService, profileringService)
                    }
                }

                val periode = TestData.nyStartetPeriode(identitetsnummer = TestData.fnr6)
                val profileringer = TestData.nyProfileringList(size = 3, periodeId = periode.id)
                periodeService.lagrePeriode(periode)
                profileringer.forEach(profileringService::lagreProfilering)

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
                coVerify { tilgangskontrollClientMock.harAnsattTilgangTilPerson(any(), any(), any()) }
            }
        }

        "/veileder/profilering med siste-flagg should return 200 OK" {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns listOf(IdentInformasjon(TestData.fnr7, IdentGruppe.FOLKEREGISTERIDENT))
            coEvery {
                tilgangskontrollClientMock.harAnsattTilgangTilPerson(any(), any(), any())
            } returns Data(true)

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        profileringRoutes(authorizationService, profileringService)
                    }
                }

                val periode = TestData.nyStartetPeriode(identitetsnummer = TestData.fnr7)
                val profileringer = TestData.nyProfileringList(size = 3, periodeId = periode.id)
                periodeService.lagrePeriode(periode)
                profileringer.forEach(profileringService::lagreProfilering)

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
                coVerify { tilgangskontrollClientMock.harAnsattTilgangTilPerson(any(), any(), any()) }
            }
        }
    }
})
