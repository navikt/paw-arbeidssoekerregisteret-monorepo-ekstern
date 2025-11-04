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
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ArbeidssoekerperiodeAggregertResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ArbeidssoekerperiodeRequest
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ArbeidssoekerperiodeResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.configureHTTP
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.configureSerialization
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.ApplicationTestContext
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.TestData
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.TestData.toProfileringResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.configureAuthentication
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.issueAzureM2MToken
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.issueAzureToken
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.issueTokenXToken
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.shouldBeEqualTo
import no.nav.paw.error.model.Data
import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.pdl.graphql.generated.enums.IdentGruppe
import no.nav.paw.pdl.graphql.generated.hentidenter.IdentInformasjon

class PerioderRoutesTest : FreeSpec({
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

        "/arbeidssoekerperioder should respond with 403 Forbidden without token" {
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

                val response = testClient.get("api/v1/arbeidssoekerperioder")

                response.status shouldBe HttpStatusCode.Forbidden
            }
        }

        "/arbeidssoekerperioder should respond with 403 Forbidden with Azure token" {
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
                    bearerAuth(mockOAuth2Server.issueAzureToken())
                }

                response.status shouldBe HttpStatusCode.Forbidden
            }
        }

        "/arbeidssoekerperioder should respond with 403 Forbidden with Azure M2M token" {
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
                    bearerAuth(mockOAuth2Server.issueAzureM2MToken())
                }

                response.status shouldBe HttpStatusCode.Forbidden
            }
        }

        "/arbeidssoekerperioder should respond with 200 OK" {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns listOf(IdentInformasjon(TestData.fnr1, IdentGruppe.FOLKEREGISTERIDENT))

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        perioderRoutes(authorizationService, periodeService)
                    }
                }

                val perioder = TestData.nyPeriodeList(size = 3, identitetsnummer = TestData.fnr1)
                perioder.forEach(periodeService::lagrePeriode)

                val testClient = configureTestClient()

                val response = testClient.get("api/v1/arbeidssoekerperioder") {
                    bearerAuth(mockOAuth2Server.issueTokenXToken())
                }

                response.status shouldBe HttpStatusCode.OK
                val periodeResponses = response.body<List<ArbeidssoekerperiodeResponse>>()
                periodeResponses.size shouldBe 3
                perioder[0] shouldBeEqualTo periodeResponses[0]
                perioder[1] shouldBeEqualTo periodeResponses[1]
                perioder[2] shouldBeEqualTo periodeResponses[2]

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
            }
        }

        "/arbeidssoekerperioder med siste-flagg should respond with 200 OK" {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns listOf(IdentInformasjon(TestData.fnr2, IdentGruppe.FOLKEREGISTERIDENT))

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        perioderRoutes(authorizationService, periodeService)
                    }
                }

                val perioder = TestData.nyPeriodeList(size = 3, identitetsnummer = TestData.fnr2)
                perioder.forEach(periodeService::lagrePeriode)

                val testClient = configureTestClient()

                val response = testClient.get("api/v1/arbeidssoekerperioder?siste=true") {
                    bearerAuth(mockOAuth2Server.issueTokenXToken())
                }

                response.status shouldBe HttpStatusCode.OK
                val periodeResponses = response.body<List<ArbeidssoekerperiodeResponse>>()
                periodeResponses.size shouldBe 1
                perioder[0] shouldBeEqualTo periodeResponses[0]

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
            }
        }

        "/arbeidssoekerperioder-aggregert should respond with 200 OK" {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns listOf(IdentInformasjon(TestData.fnr12, IdentGruppe.FOLKEREGISTERIDENT))

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        perioderRoutes(authorizationService, periodeService)
                    }
                }

                val perioder = TestData.nyPeriodeList(size = 3, identitetsnummer = TestData.fnr12)
                val opplysninger = TestData.nyOpplysningerOmArbeidssoekerList(size = 3, periodeId = perioder[0].id)
                val profileringer = listOf(
                    TestData.nyProfilering(
                        periodeId = perioder[0].id,
                        opplysningerId = opplysninger[0].id
                    ),
                    TestData.nyProfilering(
                        periodeId = perioder[0].id,
                        opplysningerId = opplysninger[1].id
                    ),
                    TestData.nyProfilering(
                        periodeId = perioder[0].id,
                        opplysningerId = opplysninger[2].id
                    )
                )

                val bekreftelser = TestData.nyBekreftelseList(size = 3, periodeId = perioder[0].id)
                perioder.forEach(periodeService::lagrePeriode)
                opplysninger.forEach(opplysningerService::lagreOpplysninger)
                profileringer.forEach(profileringService::lagreProfilering)
                bekreftelser.forEach(bekreftelseService::lagreBekreftelse)

                val testClient = configureTestClient()

                val response = testClient.get("api/v1/arbeidssoekerperioder-aggregert") {
                    bearerAuth(mockOAuth2Server.issueTokenXToken())
                }

                response.status shouldBe HttpStatusCode.OK
                val periodeResponses = response.body<List<ArbeidssoekerperiodeAggregertResponse>>()
                periodeResponses.size shouldBe 3
                perioder[0] shouldBeEqualTo periodeResponses[0]
                opplysninger[0] shouldBeEqualTo (periodeResponses[0].opplysningerOmArbeidssoeker?.get(0)
                    ?: error("Missing opplysninger"))
                profileringer[0] shouldBeEqualTo (periodeResponses[0].opplysningerOmArbeidssoeker?.get(0)?.profilering?.toProfileringResponse()
                    ?: error("Missing profilering"))
                profileringer[1] shouldBeEqualTo (periodeResponses[0].opplysningerOmArbeidssoeker?.get(1)?.profilering?.toProfileringResponse()
                    ?: error("Missing profilering"))
                profileringer[2] shouldBeEqualTo (periodeResponses[0].opplysningerOmArbeidssoeker?.get(2)?.profilering?.toProfileringResponse()
                    ?: error("Missing profilering"))
                bekreftelser[0] shouldBeEqualTo (periodeResponses[0].bekreftelser?.get(0)
                    ?: error("Missing bekreftelse"))

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
            }
        }

        "/arbeidssoekerperioder-aggregert?siste=true should respond with 200 OK" {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns listOf(IdentInformasjon(TestData.fnr13, IdentGruppe.FOLKEREGISTERIDENT))

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        perioderRoutes(authorizationService, periodeService)
                    }
                }

                val perioder = TestData.nyPeriodeList(size = 3, identitetsnummer = TestData.fnr13)
                val opplysninger = TestData.nyOpplysningerOmArbeidssoekerList(size = 3, periodeId = perioder[0].id)
                val profileringer = listOf(
                    TestData.nyProfilering(
                        periodeId = perioder[0].id,
                        opplysningerId = opplysninger[0].id
                    ),
                    TestData.nyProfilering(
                        periodeId = perioder[0].id,
                        opplysningerId = opplysninger[1].id
                    ),
                    TestData.nyProfilering(
                        periodeId = perioder[0].id,
                        opplysningerId = opplysninger[2].id
                    )
                )

                val bekreftelser = TestData.nyBekreftelseList(size = 3, periodeId = perioder[0].id)
                perioder.forEach(periodeService::lagrePeriode)
                opplysninger.forEach(opplysningerService::lagreOpplysninger)
                profileringer.forEach(profileringService::lagreProfilering)
                bekreftelser.forEach(bekreftelseService::lagreBekreftelse)

                val testClient = configureTestClient()

                val response = testClient.get("api/v1/arbeidssoekerperioder-aggregert?siste=true") {
                    bearerAuth(mockOAuth2Server.issueTokenXToken())
                }

                response.status shouldBe HttpStatusCode.OK
                val periodeResponses = response.body<List<ArbeidssoekerperiodeAggregertResponse>>()
                periodeResponses.size shouldBe 1
                perioder[0] shouldBeEqualTo periodeResponses[0]
                opplysninger[0] shouldBeEqualTo (periodeResponses[0].opplysningerOmArbeidssoeker?.get(0)
                    ?: error("Missing opplysninger"))
                profileringer[0] shouldBeEqualTo (periodeResponses[0].opplysningerOmArbeidssoeker?.get(0)?.profilering?.toProfileringResponse()
                    ?: error("Missing profilering"))
                bekreftelser[0] shouldBeEqualTo (periodeResponses[0].bekreftelser?.get(0)
                    ?: error("Missing bekreftelse"))
                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
            }
        }

        "/veileder/arbeidssoekerperioder should respond with 403 Forbidden with TokenX token" {
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
                    bearerAuth(mockOAuth2Server.issueTokenXToken())
                    contentType(ContentType.Application.Json)
                    setBody(
                        ArbeidssoekerperiodeRequest(
                            identitetsnummer = TestData.fnr3
                        )
                    )
                }

                response.status shouldBe HttpStatusCode.Forbidden
            }
        }

        "/veileder/arbeidssoekerperioder should return 403 Forbidden uten tilgang fra Tilgangskontroll" {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns listOf(IdentInformasjon(TestData.fnr3, IdentGruppe.FOLKEREGISTERIDENT))
            coEvery {
                tilgangskontrollClientMock.harAnsattTilgangTilPerson(any(), any(), any())
            } returns Data(false)

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
                            identitetsnummer = TestData.fnr3
                        )
                    )
                }

                response.status shouldBe HttpStatusCode.Forbidden

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                coVerify { tilgangskontrollClientMock.harAnsattTilgangTilPerson(any(), any(), any()) }
            }
        }

        "/veileder/arbeidssoekerperioder should return 200 OK" {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns listOf(IdentInformasjon(TestData.fnr4, IdentGruppe.FOLKEREGISTERIDENT))
            coEvery {
                tilgangskontrollClientMock.harAnsattTilgangTilPerson(any(), any(), any())
            } returns Data(true)

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        perioderRoutes(authorizationService, periodeService)
                    }
                }

                val perioder = TestData.nyPeriodeList(size = 3, identitetsnummer = TestData.fnr4)
                perioder.forEach(periodeService::lagrePeriode)

                val testClient = configureTestClient()

                val response = testClient.post("api/v1/veileder/arbeidssoekerperioder") {
                    bearerAuth(mockOAuth2Server.issueAzureToken())
                    contentType(ContentType.Application.Json)
                    setBody(
                        ArbeidssoekerperiodeRequest(
                            identitetsnummer = TestData.fnr4
                        )
                    )
                }

                response.status shouldBe HttpStatusCode.OK
                val periodeResponses = response.body<List<ArbeidssoekerperiodeResponse>>()
                periodeResponses.size shouldBe 3
                perioder[0] shouldBeEqualTo periodeResponses[0]
                perioder[1] shouldBeEqualTo periodeResponses[1]
                perioder[2] shouldBeEqualTo periodeResponses[2]

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                coVerify { tilgangskontrollClientMock.harAnsattTilgangTilPerson(any(), any(), any()) }
            }
        }

        "/veileder/arbeidssoekerperioder med siste-flagg should return 200 OK" {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns listOf(IdentInformasjon(TestData.fnr5, IdentGruppe.FOLKEREGISTERIDENT))
            coEvery {
                tilgangskontrollClientMock.harAnsattTilgangTilPerson(any(), any(), any())
            } returns Data(true)

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        perioderRoutes(authorizationService, periodeService)
                    }
                }

                val perioder = TestData.nyPeriodeList(size = 3, identitetsnummer = TestData.fnr5)
                perioder.forEach(periodeService::lagrePeriode)

                val testClient = configureTestClient()

                val response = testClient.post("api/v1/veileder/arbeidssoekerperioder?siste=true") {
                    bearerAuth(mockOAuth2Server.issueAzureToken())
                    contentType(ContentType.Application.Json)
                    setBody(
                        ArbeidssoekerperiodeRequest(
                            identitetsnummer = TestData.fnr5
                        )
                    )
                }

                response.status shouldBe HttpStatusCode.OK
                val periodeResponses = response.body<List<ArbeidssoekerperiodeResponse>>()
                periodeResponses.size shouldBe 1
                perioder[0] shouldBeEqualTo periodeResponses[0]

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                coVerify { tilgangskontrollClientMock.harAnsattTilgangTilPerson(any(), any(), any()) }
            }
        }

        "/veileder/arbeidssoekerperioder-aggregert should return 200 OK" {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns listOf(IdentInformasjon(TestData.fnr14, IdentGruppe.FOLKEREGISTERIDENT))
            coEvery {
                tilgangskontrollClientMock.harAnsattTilgangTilPerson(any(), any(), any())
            } returns Data(true)

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        perioderRoutes(authorizationService, periodeService)
                    }
                }

                val perioder = TestData.nyPeriodeList(size = 3, identitetsnummer = TestData.fnr14)
                val opplysninger = TestData.nyOpplysningerOmArbeidssoekerList(size = 3, periodeId = perioder[0].id)
                val profileringer = listOf(
                    TestData.nyProfilering(
                        periodeId = perioder[0].id,
                        opplysningerId = opplysninger[0].id
                    ),
                    TestData.nyProfilering(
                        periodeId = perioder[0].id,
                        opplysningerId = opplysninger[1].id
                    ),
                    TestData.nyProfilering(
                        periodeId = perioder[0].id,
                        opplysningerId = opplysninger[2].id
                    )
                )
                val bekreftelser = TestData.nyBekreftelseList(size = 3, periodeId = perioder[0].id)
                perioder.forEach(periodeService::lagrePeriode)
                opplysninger.forEach(opplysningerService::lagreOpplysninger)
                profileringer.forEach(profileringService::lagreProfilering)
                bekreftelser.forEach(bekreftelseService::lagreBekreftelse)

                val testClient = configureTestClient()

                val response = testClient.post("api/v1/veileder/arbeidssoekerperioder-aggregert") {
                    bearerAuth(mockOAuth2Server.issueAzureToken())
                    contentType(ContentType.Application.Json)
                    setBody(
                        ArbeidssoekerperiodeRequest(
                            identitetsnummer = TestData.fnr5
                        )
                    )
                }

                response.status shouldBe HttpStatusCode.OK
                val periodeResponses = response.body<List<ArbeidssoekerperiodeAggregertResponse>>()
                periodeResponses.size shouldBe 3
                perioder[0] shouldBeEqualTo periodeResponses[0]
                opplysninger[0] shouldBeEqualTo (periodeResponses[0].opplysningerOmArbeidssoeker?.get(0)
                    ?: error("Missing opplysninger"))
                profileringer[0] shouldBeEqualTo (periodeResponses[0].opplysningerOmArbeidssoeker?.get(0)?.profilering?.toProfileringResponse()
                    ?: error("Missing profilering"))
                profileringer[1] shouldBeEqualTo (periodeResponses[0].opplysningerOmArbeidssoeker?.get(1)?.profilering?.toProfileringResponse()
                    ?: error("Missing profilering"))
                profileringer[2] shouldBeEqualTo (periodeResponses[0].opplysningerOmArbeidssoeker?.get(2)?.profilering?.toProfileringResponse()
                    ?: error("Missing profilering"))
                bekreftelser[0] shouldBeEqualTo (periodeResponses[0].bekreftelser?.get(0)
                    ?: error("Missing bekreftelse"))
                //egenvurderinger[0] shouldBeEqualTo (periodeResponses[0].opplysningerOmArbeidssoeker?.get(2)?.profilering?.egenvurderinger?.get(0) ?: error("Missing egenvurdering"))

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                coVerify { tilgangskontrollClientMock.harAnsattTilgangTilPerson(any(), any(), any()) }
            }
        }

        "/veileder/arbeidssoekerperioder-aggregert?siste=true should return 200 OK" {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns listOf(IdentInformasjon(TestData.fnr15, IdentGruppe.FOLKEREGISTERIDENT))
            coEvery {
                tilgangskontrollClientMock.harAnsattTilgangTilPerson(any(), any(), any())
            } returns Data(true)

            testApplication {
                application {
                    configureAuthentication(mockOAuth2Server)
                    configureSerialization()
                    configureHTTP()
                    routing {
                        perioderRoutes(authorizationService, periodeService)
                    }
                }

                val perioder = TestData.nyPeriodeList(size = 3, identitetsnummer = TestData.fnr15)
                val opplysninger = TestData.nyOpplysningerOmArbeidssoekerList(size = 3, periodeId = perioder[0].id)
                val profileringer = listOf(
                    TestData.nyProfilering(
                        periodeId = perioder[0].id,
                        opplysningerId = opplysninger[0].id
                    ),
                    TestData.nyProfilering(
                        periodeId = perioder[0].id,
                        opplysningerId = opplysninger[1].id
                    ),
                    TestData.nyProfilering(
                        periodeId = perioder[0].id,
                        opplysningerId = opplysninger[2].id
                    )
                )

                val bekreftelser = TestData.nyBekreftelseList(size = 3, periodeId = perioder[0].id)
                perioder.forEach(periodeService::lagrePeriode)
                opplysninger.forEach(opplysningerService::lagreOpplysninger)
                profileringer.forEach(profileringService::lagreProfilering)
                bekreftelser.forEach(bekreftelseService::lagreBekreftelse)

                val testClient = configureTestClient()

                val response = testClient.post("api/v1/veileder/arbeidssoekerperioder-aggregert?siste=true") {
                    bearerAuth(mockOAuth2Server.issueAzureToken())
                    contentType(ContentType.Application.Json)
                    setBody(
                        ArbeidssoekerperiodeRequest(
                            identitetsnummer = TestData.fnr5
                        )
                    )
                }

                response.status shouldBe HttpStatusCode.OK
                val periodeResponses = response.body<List<ArbeidssoekerperiodeAggregertResponse>>()
                periodeResponses.size shouldBe 1
                perioder[0] shouldBeEqualTo periodeResponses[0]
                opplysninger[0] shouldBeEqualTo (periodeResponses[0].opplysningerOmArbeidssoeker?.get(0)
                    ?: error("Missing opplysninger"))
                profileringer[0] shouldBeEqualTo (periodeResponses[0].opplysningerOmArbeidssoeker?.get(0)?.profilering?.toProfileringResponse()
                    ?: error("Missing profilering"))
                profileringer[1] shouldBeEqualTo (periodeResponses[0].opplysningerOmArbeidssoeker?.get(1)?.profilering?.toProfileringResponse()
                    ?: error("Missing profilering"))
                profileringer[2] shouldBeEqualTo (periodeResponses[0].opplysningerOmArbeidssoeker?.get(2)?.profilering?.toProfileringResponse()
                    ?: error("Missing profilering"))
                bekreftelser[0] shouldBeEqualTo (periodeResponses[0].bekreftelser?.get(0)
                    ?: error("Missing bekreftelse"))
                //egenvurderinger[0] shouldBeEqualTo (periodeResponses[0].opplysningerOmArbeidssoeker?.get(2)?.profilering?.egenvurderinger?.get(0) ?: error("Missing egenvurdering"))

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                coVerify { tilgangskontrollClientMock.harAnsattTilgangTilPerson(any(), any(), any()) }
            }
        }


        "/veileder/arbeidssoekerperioder should return 200 OK med M2M-token" {
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
                        perioderRoutes(authorizationService, periodeService)
                    }
                }

                val perioder = TestData.nyPeriodeList(size = 3, identitetsnummer = TestData.fnr6)
                perioder.forEach(periodeService::lagrePeriode)

                val testClient = configureTestClient()

                val response = testClient.post("api/v1/veileder/arbeidssoekerperioder") {
                    bearerAuth(mockOAuth2Server.issueAzureM2MToken())
                    contentType(ContentType.Application.Json)
                    setBody(
                        ArbeidssoekerperiodeRequest(
                            identitetsnummer = TestData.fnr6
                        )
                    )
                }

                response.status shouldBe HttpStatusCode.OK
                val periodeResponses = response.body<List<ArbeidssoekerperiodeResponse>>()
                periodeResponses.size shouldBe 3
                perioder[0] shouldBeEqualTo periodeResponses[0]
                perioder[1] shouldBeEqualTo periodeResponses[1]
                perioder[2] shouldBeEqualTo periodeResponses[2]

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                coVerify { tilgangskontrollClientMock.harAnsattTilgangTilPerson(any(), any(), any()) }
            }
        }
    }
})
