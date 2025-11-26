package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.route

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import io.mockk.Called
import io.mockk.every
import io.mockk.verify
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.exception.DIALOG_IKKE_FUNNET_ERROR_TYPE
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.model.EgenvurderingDialogRequest
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.model.EgenvurderingDialogResponse
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.test.TestContext
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.test.validateAgainstOpenApiSpec
import no.nav.paw.error.model.ErrorType
import no.nav.paw.error.model.ProblemDetails
import java.util.*

class EgenvurderingRoutesTest : FreeSpec({
    with(TestContext.build()) {

        beforeSpec {
            mockOAuth2Server.start()
        }

        afterSpec {
            mockOAuth2Server.shutdown()
        }

        afterTest {
            verify { veilarbdialogClientMock wasNot Called }
        }

        "Test suite for hent dialog" - {
            securedTestApplication {
                val client = buildTestClient()

                "Skal få 400 ved ugyldig request" {
                    // GIVEN
                    val token = mockOAuth2Server.issueAzureAdToken()
                    val request = "foo" to "bar"

                    // WHEN
                    val response = client.post("/api/v1/egenvurdering/dialog") {
                        bearerAuth(token.serialize())
                        setJsonBody(request)
                    }

                    // THEN
                    response.validateAgainstOpenApiSpec()
                    response.status shouldBe HttpStatusCode.BadRequest
                    val body = response.body<ProblemDetails>()
                    body.type shouldBe ErrorType.domain("http").error("kunne-ikke-tolke-forespoersel").build()
                    body.status shouldBe HttpStatusCode.BadRequest
                    body.title shouldBe HttpStatusCode.BadRequest.description

                    verify { periodeIdDialogIdRepositoryMock wasNot Called }
                }

                "Skal få 403 ved ugyldig token" {
                    // GIVEN
                    val token = mockOAuth2Server.issueTokenXToken()
                    val request = EgenvurderingDialogRequest(periodeId = UUID.randomUUID())

                    // WHEN
                    val response = client.post("/api/v1/egenvurdering/dialog") {
                        bearerAuth(token.serialize())
                        setJsonBody(request)
                    }

                    // THEN
                    response.validateAgainstOpenApiSpec()
                    response.status shouldBe HttpStatusCode.Forbidden
                    val body = response.body<ProblemDetails>()
                    body.type shouldBe ErrorType.domain("security").error("ugyldig-bearer-token").build()
                    body.status shouldBe HttpStatusCode.Forbidden
                    body.title shouldBe HttpStatusCode.Forbidden.description

                    verify { periodeIdDialogIdRepositoryMock wasNot Called }
                }

                "Skal få 404 ved ingen dialog" {
                    // GIVEN
                    val token = mockOAuth2Server.issueAzureAdToken()
                    every { periodeIdDialogIdRepositoryMock.getDialogIdOrNull(any()) } returns null
                    val request = EgenvurderingDialogRequest(periodeId = UUID.randomUUID())

                    // WHEN
                    val response = client.post("/api/v1/egenvurdering/dialog") {
                        bearerAuth(token.serialize())
                        setJsonBody(request)
                    }

                    // THEN
                    response.validateAgainstOpenApiSpec()
                    response.status shouldBe HttpStatusCode.NotFound
                    val body = response.body<ProblemDetails>()
                    body.type shouldBe DIALOG_IKKE_FUNNET_ERROR_TYPE
                    body.status shouldBe HttpStatusCode.NotFound
                    body.title shouldBe HttpStatusCode.NotFound.description

                    verify { periodeIdDialogIdRepositoryMock.getDialogIdOrNull(any()) }
                }

                "Skal få 200 ved dialog" {
                    // GIVEN
                    val token = mockOAuth2Server.issueAzureAdToken()
                    every { periodeIdDialogIdRepositoryMock.getDialogIdOrNull(any()) } returns 1001L
                    val request = EgenvurderingDialogRequest(periodeId = UUID.randomUUID())

                    // WHEN
                    val response = client.post("/api/v1/egenvurdering/dialog") {
                        bearerAuth(token.serialize())
                        setJsonBody(request)
                    }

                    // THEN
                    response.validateAgainstOpenApiSpec()
                    response.status shouldBe HttpStatusCode.OK
                    val body = response.body<EgenvurderingDialogResponse>()
                    body.dialogId shouldBe 1001L

                    verify { periodeIdDialogIdRepositoryMock.getDialogIdOrNull(any()) }
                }
            }
        }
    }
})