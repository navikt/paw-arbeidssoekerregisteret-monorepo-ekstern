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
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.SamletInformasjonRequest
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.SamletInformasjonResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.configureHTTP
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.configureSerialization
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.ApplicationTestContext
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.TestData
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.configureAuthentication
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.issueAzureToken
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.issueTokenXToken
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.shouldBeEqualTo
import no.nav.paw.pdl.graphql.generated.enums.IdentGruppe
import no.nav.paw.pdl.graphql.generated.hentidenter.IdentInformasjon
import no.nav.paw.security.authentication.model.Identitetsnummer
import no.nav.poao_tilgang.client.Decision
import no.nav.poao_tilgang.client.PolicyRequest
import no.nav.poao_tilgang.client.PolicyResult
import java.time.Duration
import java.time.Instant
import java.util.*

class SamletInformasjonRoutesTest : FreeSpec({
    with(ApplicationTestContext.withRealDataAccess()) {

        beforeSpec {
            mockOAuth2Server.start()
        }

        afterSpec {
            mockOAuth2Server.shutdown()
            confirmVerified(
                pdlHttpConsumerMock,
                poaoTilgangHttpConsumerMock,
            )
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
                            profileringService,
                            bekreftelseService
                        )
                    }
                }

                val testClient = configureTestClient()

                val response = testClient.get("api/v1/samlet-informasjon")

                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        "/samlet-informasjon should return 200 OK" {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns listOf(IdentInformasjon(TestData.fnr1, IdentGruppe.FOLKEREGISTERIDENT))

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
                            profileringService,
                            bekreftelseService
                        )
                    }
                }

                val perioder = TestData.nyPeriodeList(size = 3, identitetsnummer = TestData.fnr1)
                val opplysinger = perioder.mapIndexed { index, periode ->
                    val sendtInnAv = TestData
                        .nyMetadata(tidspunkt = Instant.now().minus(Duration.ofDays(index.toLong())))
                    TestData.nyOpplysningerOmArbeidssoeker(periodeId = periode.id, sendtInnAv = sendtInnAv)
                }
                val profileringer = perioder.mapIndexed { index, periode ->
                    val sendtInnAv =
                        TestData.nyMetadata(tidspunkt = Instant.now().minus(Duration.ofDays(index.toLong())))
                    TestData.nyProfilering(periodeId = periode.id, sendtInAv = sendtInnAv)
                }
                val bekreftelser = perioder.mapIndexed { index, periode ->
                    val gjelderFra = Instant.now().minus(Duration.ofDays(index.toLong()))
                    val svar = TestData.nyBekreftelseSvar(
                        gjelderFra = gjelderFra,
                        gjelderTil = gjelderFra.plus(Duration.ofDays(14))
                    )
                    TestData.nyBekreftelse(periodeId = periode.id, svar = svar)
                }
                periodeService.lagreAllePerioder(perioder)
                opplysningerService.lagreAlleOpplysninger(opplysinger)
                profileringService.lagreAlleProfileringer(profileringer)
                bekreftelseService.lagreAlleBekreftelser(bekreftelser)

                val testClient = configureTestClient()

                val response = testClient.get("api/v1/samlet-informasjon") {
                    bearerAuth(mockOAuth2Server.issueTokenXToken(pid = TestData.fnr1))
                }

                response.status shouldBe HttpStatusCode.OK
                val samletInfo = response.body<SamletInformasjonResponse>()
                samletInfo.arbeidssoekerperioder.size shouldBe 3
                samletInfo.opplysningerOmArbeidssoeker.size shouldBe 3
                samletInfo.profilering.size shouldBe 3
                samletInfo.bekreftelser.size shouldBe 3
                perioder[0] shouldBeEqualTo samletInfo.arbeidssoekerperioder[0]
                perioder[1] shouldBeEqualTo samletInfo.arbeidssoekerperioder[1]
                perioder[2] shouldBeEqualTo samletInfo.arbeidssoekerperioder[2]
                opplysinger[0] shouldBeEqualTo samletInfo.opplysningerOmArbeidssoeker[0]
                opplysinger[1] shouldBeEqualTo samletInfo.opplysningerOmArbeidssoeker[1]
                opplysinger[2] shouldBeEqualTo samletInfo.opplysningerOmArbeidssoeker[2]
                profileringer[0] shouldBeEqualTo samletInfo.profilering[0]
                profileringer[1] shouldBeEqualTo samletInfo.profilering[1]
                profileringer[2] shouldBeEqualTo samletInfo.profilering[2]
                bekreftelser[0] shouldBeEqualTo samletInfo.bekreftelser[0]
                bekreftelser[1] shouldBeEqualTo samletInfo.bekreftelser[1]
                bekreftelser[2] shouldBeEqualTo samletInfo.bekreftelser[2]

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
            }
        }

        "/samlet-informasjon med siste-flagg should return 200 OK" {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns listOf(IdentInformasjon(TestData.fnr2, IdentGruppe.FOLKEREGISTERIDENT))

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
                            profileringService,
                            bekreftelseService
                        )
                    }
                }

                val perioder = TestData.nyPeriodeList(size = 3, identitetsnummer = TestData.fnr2)
                val opplysinger = perioder.mapIndexed { index, periode ->
                    val sendtInnAv = TestData
                        .nyMetadata(tidspunkt = Instant.now().minus(Duration.ofDays(index.toLong())))
                    TestData.nyOpplysningerOmArbeidssoeker(periodeId = periode.id, sendtInnAv = sendtInnAv)
                }
                val profileringer = perioder.mapIndexed { index, periode ->
                    val sendtInnAv =
                        TestData.nyMetadata(tidspunkt = Instant.now().minus(Duration.ofDays(index.toLong())))
                    TestData.nyProfilering(periodeId = periode.id, sendtInAv = sendtInnAv)
                }
                val bekreftelser = perioder.mapIndexed { index, periode ->
                    val gjelderFra = Instant.now().minus(Duration.ofDays(index.toLong()))
                    val svar = TestData.nyBekreftelseSvar(
                        gjelderFra = gjelderFra,
                        gjelderTil = gjelderFra.plus(Duration.ofDays(14))
                    )
                    TestData.nyBekreftelse(periodeId = periode.id, svar = svar)
                }
                periodeService.lagreAllePerioder(perioder)
                opplysningerService.lagreAlleOpplysninger(opplysinger)
                profileringService.lagreAlleProfileringer(profileringer)
                bekreftelseService.lagreAlleBekreftelser(bekreftelser)

                val testClient = configureTestClient()

                val response = testClient.get("api/v1/samlet-informasjon?siste=true") {
                    bearerAuth(mockOAuth2Server.issueTokenXToken(pid = TestData.fnr2))
                }

                response.status shouldBe HttpStatusCode.OK
                val samletInfo = response.body<SamletInformasjonResponse>()
                samletInfo.arbeidssoekerperioder.size shouldBe 1
                samletInfo.opplysningerOmArbeidssoeker.size shouldBe 1
                samletInfo.profilering.size shouldBe 1
                samletInfo.bekreftelser.size shouldBe 1
                perioder[0] shouldBeEqualTo samletInfo.arbeidssoekerperioder[0]
                opplysinger[0] shouldBeEqualTo samletInfo.opplysningerOmArbeidssoeker[0]
                profileringer[0] shouldBeEqualTo samletInfo.profilering[0]
                bekreftelser[0] shouldBeEqualTo samletInfo.bekreftelser[0]

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
            }
        }

        "/veileder/samlet-informasjon should return 403 uten POAO Tilgang" {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns listOf(IdentInformasjon(TestData.fnr3, IdentGruppe.FOLKEREGISTERIDENT))
            every {
                poaoTilgangHttpConsumerMock.evaluatePolicies(any<List<PolicyRequest>>())
            } returns listOf(PolicyResult(UUID.randomUUID(), Decision.Deny("test", "test")))

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
                            profileringService,
                            bekreftelseService
                        )
                    }
                }

                val testClient = configureTestClient()

                val response = testClient.post("api/v1/veileder/samlet-informasjon") {
                    bearerAuth(mockOAuth2Server.issueAzureToken())
                    contentType(ContentType.Application.Json)
                    setBody(
                        SamletInformasjonRequest(
                            identitetsnummer = TestData.fnr3
                        )
                    )
                }

                response.status shouldBe HttpStatusCode.Forbidden

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                verify { poaoTilgangHttpConsumerMock.evaluatePolicies(any<List<PolicyRequest>>()) }
            }
        }

        "/veileder/samlet-informasjon should return 200 OK" {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns listOf(IdentInformasjon(TestData.fnr4, IdentGruppe.FOLKEREGISTERIDENT))
            every {
                poaoTilgangHttpConsumerMock.evaluatePolicies(any<List<PolicyRequest>>())
            } returns listOf(PolicyResult(UUID.randomUUID(), Decision.Permit))

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
                            profileringService,
                            bekreftelseService
                        )
                    }
                }

                val perioder = TestData.nyPeriodeList(size = 3, identitetsnummer = TestData.fnr4)
                val opplysinger = perioder.mapIndexed { index, periode ->
                    val sendtInnAv = TestData
                        .nyMetadata(tidspunkt = Instant.now().minus(Duration.ofDays(index.toLong())))
                    TestData.nyOpplysningerOmArbeidssoeker(periodeId = periode.id, sendtInnAv = sendtInnAv)
                }
                val profileringer = perioder.mapIndexed { index, periode ->
                    val sendtInnAv =
                        TestData.nyMetadata(tidspunkt = Instant.now().minus(Duration.ofDays(index.toLong())))
                    TestData.nyProfilering(periodeId = periode.id, sendtInAv = sendtInnAv)
                }
                val bekreftelser = perioder.mapIndexed { index, periode ->
                    val gjelderFra = Instant.now().minus(Duration.ofDays(index.toLong()))
                    val svar = TestData.nyBekreftelseSvar(
                        gjelderFra = gjelderFra,
                        gjelderTil = gjelderFra.plus(Duration.ofDays(14))
                    )
                    TestData.nyBekreftelse(periodeId = periode.id, svar = svar)
                }
                periodeService.lagreAllePerioder(perioder)
                opplysningerService.lagreAlleOpplysninger(opplysinger)
                profileringService.lagreAlleProfileringer(profileringer)
                bekreftelseService.lagreAlleBekreftelser(bekreftelser)

                val testClient = configureTestClient()

                val response = testClient.post("api/v1/veileder/samlet-informasjon") {
                    bearerAuth(mockOAuth2Server.issueAzureToken())
                    contentType(ContentType.Application.Json)
                    setBody(
                        SamletInformasjonRequest(
                            identitetsnummer = TestData.fnr4
                        )
                    )
                }

                response.status shouldBe HttpStatusCode.OK
                val samletInfo = response.body<SamletInformasjonResponse>()
                samletInfo.arbeidssoekerperioder.size shouldBe 3
                samletInfo.opplysningerOmArbeidssoeker.size shouldBe 3
                samletInfo.profilering.size shouldBe 3
                samletInfo.bekreftelser.size shouldBe 3
                perioder[0] shouldBeEqualTo samletInfo.arbeidssoekerperioder[0]
                perioder[1] shouldBeEqualTo samletInfo.arbeidssoekerperioder[1]
                perioder[2] shouldBeEqualTo samletInfo.arbeidssoekerperioder[2]
                opplysinger[0] shouldBeEqualTo samletInfo.opplysningerOmArbeidssoeker[0]
                opplysinger[1] shouldBeEqualTo samletInfo.opplysningerOmArbeidssoeker[1]
                opplysinger[2] shouldBeEqualTo samletInfo.opplysningerOmArbeidssoeker[2]
                profileringer[0] shouldBeEqualTo samletInfo.profilering[0]
                profileringer[1] shouldBeEqualTo samletInfo.profilering[1]
                profileringer[2] shouldBeEqualTo samletInfo.profilering[2]
                bekreftelser[0] shouldBeEqualTo samletInfo.bekreftelser[0]
                bekreftelser[1] shouldBeEqualTo samletInfo.bekreftelser[1]
                bekreftelser[2] shouldBeEqualTo samletInfo.bekreftelser[2]

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                verify { poaoTilgangHttpConsumerMock.evaluatePolicies(any<List<PolicyRequest>>()) }
            }
        }

        "/veileder/samlet-informasjon med siste-flagg should return 200 OK" {
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns listOf(IdentInformasjon(TestData.fnr5, IdentGruppe.FOLKEREGISTERIDENT))
            every {
                poaoTilgangHttpConsumerMock.evaluatePolicies(any<List<PolicyRequest>>())
            } returns listOf(PolicyResult(UUID.randomUUID(), Decision.Permit))

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
                            profileringService,
                            bekreftelseService
                        )
                    }
                }

                val perioder = TestData.nyPeriodeList(size = 3, identitetsnummer = TestData.fnr5)
                val opplysinger = perioder.mapIndexed { index, periode ->
                    val sendtInnAv = TestData
                        .nyMetadata(tidspunkt = Instant.now().minus(Duration.ofDays(index.toLong())))
                    TestData.nyOpplysningerOmArbeidssoeker(periodeId = periode.id, sendtInnAv = sendtInnAv)
                }
                val profileringer = perioder.mapIndexed { index, periode ->
                    val sendtInnAv =
                        TestData.nyMetadata(tidspunkt = Instant.now().minus(Duration.ofDays(index.toLong())))
                    TestData.nyProfilering(periodeId = periode.id, sendtInAv = sendtInnAv)
                }
                val bekreftelser = perioder.mapIndexed { index, periode ->
                    val gjelderFra = Instant.now().minus(Duration.ofDays(index.toLong()))
                    val svar = TestData.nyBekreftelseSvar(
                        gjelderFra = gjelderFra,
                        gjelderTil = gjelderFra.plus(Duration.ofDays(14))
                    )
                    TestData.nyBekreftelse(periodeId = periode.id, svar = svar)
                }
                periodeService.lagreAllePerioder(perioder)
                opplysningerService.lagreAlleOpplysninger(opplysinger)
                profileringService.lagreAlleProfileringer(profileringer)
                bekreftelseService.lagreAlleBekreftelser(bekreftelser)

                val testClient = configureTestClient()

                val response = testClient.post("api/v1/veileder/samlet-informasjon?siste=true") {
                    bearerAuth(mockOAuth2Server.issueAzureToken())
                    contentType(ContentType.Application.Json)
                    setBody(
                        SamletInformasjonRequest(
                            identitetsnummer = TestData.fnr5
                        )
                    )
                }

                response.status shouldBe HttpStatusCode.OK
                val samletInfo = response.body<SamletInformasjonResponse>()
                samletInfo.arbeidssoekerperioder.size shouldBe 1
                samletInfo.opplysningerOmArbeidssoeker.size shouldBe 1
                samletInfo.profilering.size shouldBe 1
                samletInfo.bekreftelser.size shouldBe 1
                perioder[0] shouldBeEqualTo samletInfo.arbeidssoekerperioder[0]
                opplysinger[0] shouldBeEqualTo samletInfo.opplysningerOmArbeidssoeker[0]
                profileringer[0] shouldBeEqualTo samletInfo.profilering[0]
                bekreftelser[0] shouldBeEqualTo samletInfo.bekreftelser[0]

                coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
                verify { poaoTilgangHttpConsumerMock.evaluatePolicies(any<List<PolicyRequest>>()) }
            }
        }
    }
})
