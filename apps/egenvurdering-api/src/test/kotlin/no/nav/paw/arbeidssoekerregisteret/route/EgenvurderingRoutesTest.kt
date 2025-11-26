package no.nav.paw.arbeidssoekerregisteret.route

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import no.nav.paw.arbeidssoekerregisteret.model.EgenvurderingGrunnlag
import no.nav.paw.arbeidssoekerregisteret.model.EgenvurderingRequest
import no.nav.paw.arbeidssoekerregisteret.model.EgenvurdertTil
import no.nav.paw.arbeidssoekerregisteret.model.Profilering
import no.nav.paw.arbeidssoekerregisteret.model.ProfilertTil
import no.nav.paw.arbeidssoekerregisteret.test.TestContext
import no.nav.paw.arbeidssoekerregisteret.test.validateAgainstOpenApiSpec
import no.nav.paw.error.model.ProblemDetails
import no.nav.paw.error.model.asHttpErrorType
import java.util.*

class EgenvurderingRoutesTest : FreeSpec({
    with(TestContext()) {
        beforeSpec { mockOAuth2Server.start() }
        afterSpec { mockOAuth2Server.shutdown() }

        "Test suite for hent egenvurdering-grunnlag" - {
            "Skal få 403 Forbidden med ugyldig token" {
                testApplication {
                    configureTestApplication()
                    val token = mockOAuth2Server.issueToken(
                        claims = mapOf(
                            "acr" to "Level4",
                        )
                    )

                    val client = configureTestClient()

                    val response = client.get(egenvurderingGrunnlagPath) {
                        bearerAuth(token.serialize())
                    }
                    response.validateAgainstOpenApiSpec()
                    response.status shouldBe HttpStatusCode.Forbidden
                    response.headers["x-trace-id"] shouldNotBe null
                }
            }

            "Skal få 200 OK med grunnlag fra profilering" {
                testApplication {
                    configureTestApplication()
                    val client = configureTestClient()
                    val token = mockOAuth2Server.issueTokenXToken()

                    val profileringId = UUID.randomUUID()
                    val egenvurderingGrunnlag = EgenvurderingGrunnlag(
                        grunnlag = Profilering(
                            profileringId = profileringId,
                            profilertTil = ProfilertTil.ANTATT_GODE_MULIGHETER,
                        )
                    )
                    coEvery { egenvurderingService.getEgenvurderingGrunnlag(any()) } returns egenvurderingGrunnlag

                    val response = client.get(egenvurderingGrunnlagPath) {
                        bearerAuth(token.serialize())
                    }
                    response.validateAgainstOpenApiSpec()
                    response.status shouldBe HttpStatusCode.OK
                    response.body<EgenvurderingGrunnlag>() shouldBe egenvurderingGrunnlag
                    response.headers["x-trace-id"] shouldNotBe null
                }
            }

            "Skal få 200 OK uten grunnlag fra profilering" {
                testApplication {
                    configureTestApplication()
                    val client = configureTestClient()
                    val token = mockOAuth2Server.issueTokenXToken()

                    coEvery { egenvurderingService.getEgenvurderingGrunnlag(any()) } returns EgenvurderingGrunnlag(
                        grunnlag = null
                    )

                    val response = client.get(egenvurderingGrunnlagPath) {
                        bearerAuth(token.serialize())
                    }
                    response.validateAgainstOpenApiSpec()
                    response.status shouldBe HttpStatusCode.OK
                    response.body<EgenvurderingGrunnlag>() shouldBe EgenvurderingGrunnlag(grunnlag = null)
                    response.headers["x-trace-id"] shouldNotBe null
                }
            }
        }

        "Test suite for registrering av egenvurdering" - {
            "Skal få 202 Accepted for korrekt egenvurdring" {
                testApplication {
                    configureTestApplication()
                    val client = configureTestClient()
                    val token = mockOAuth2Server.issueTokenXToken()

                    val request = EgenvurderingRequest(
                        profileringId = UUID.randomUUID(),
                        egenvurdering = EgenvurdertTil.ANTATT_GODE_MULIGHETER
                    )

                    coEvery { egenvurderingService.publiserOgLagreEgenvurdering(any(), any(), any()) } just Runs
                    val response = client.post(egenvurderingPath) {
                        setJsonBody(request)
                        bearerAuth(token.serialize())
                    }
                    response.validateAgainstOpenApiSpec()
                    response.status shouldBe HttpStatusCode.Accepted
                    response.headers["x-trace-id"] shouldNotBe null
                }
            }

            "Skal få 400 BadRequest ved ugyldig json" {
                testApplication {
                    configureTestApplication()
                    val client = configureTestClient()
                    val token = mockOAuth2Server.issueTokenXToken()

                    val response = client.post(egenvurderingPath) {
                        setJsonBody("""{ugyldigJson}""")
                        bearerAuth(token.serialize())
                    }

                    response.validateAgainstOpenApiSpec()
                    response.status shouldBe HttpStatusCode.BadRequest
                    response.headers["x-trace-id"] shouldNotBe null
                    val problem = response.body<ProblemDetails>()
                    problem.type shouldBe "kunne-ikke-tolke-forespoersel".asHttpErrorType()
                    problem.status shouldBe HttpStatusCode.BadRequest
                    problem.title shouldBe HttpStatusCode.BadRequest.description
                    problem.instance shouldBe egenvurderingPath
                    problem.detail?.isBlank() shouldBe false
                }
            }
        }
    }
})