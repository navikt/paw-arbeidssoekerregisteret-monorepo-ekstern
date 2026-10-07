package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.kontrakt

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.testing.ApplicationTestBuilder
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.client.VeilarbdialogClient
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.config.VeilarbdialogClientConfig
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.kontrakt.JsonKontrakt.NOEYTRALT_PROBLEM_TIDSPUNKT
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.kontrakt.JsonKontrakt.NOEYTRAL_PROBLEM_ID
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.kontrakt.JsonKontrakt.lesFasit
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.kontrakt.JsonKontrakt.noeytraliserProblemDetails
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.kontrakt.JsonKontrakt.skalVaereLikFasit
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.model.DialogId
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.model.DialogRequest
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.model.EgenvurderingDialogResponse
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.repository.PeriodeDialogAuditRow
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.repository.PeriodeDialogRow
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.route.DIALOG_IKKE_FUNNET_ERROR_TYPE
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.route.EGENVURDERING_DIALOG_TJENESTE_INTERNAL_SERVER_ERROR
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.service.DialogService
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.test.TestContext
import no.nav.paw.client.factory.createHttpClient
import no.nav.paw.error.model.ProblemDetails
import no.nav.paw.error.model.asHttpErrorType
import no.nav.paw.security.texas.TexasClient
import no.nav.paw.security.texas.m2m.MachineToMachineTokenResponse
import java.time.Instant
import java.util.*

/**
 * JSON-kontrakttest for egenvurdering-dialog-tjeneste.
 *
 * - Respons fra `POST /api/v1/egenvurdering/dialog` går gjennom samme plugin-oppsett som produksjon
 *   (`installContentNegotiationPlugin { configureJacksonOverrides() }` og `installErrorHandlingPlugin()`).
 * - Utgående [DialogRequest] til veilarbdialog sendes med ekte [VeilarbdialogClient] og ekte
 *   `createHttpClient()`, der bare motoren er byttet ut med en [MockEngine] som fanger bodyen.
 *
 * Fasitfilene i `src/test/resources/json-kontrakt/egenvurdering-dialog/` er generert fra
 * produksjonskoden og må aldri endres. Feiler testen, har kontrakten endret seg.
 *
 * ProblemDetails har tilfeldig `id` og `timestamp`. Disse erstattes med faste verdier
 * ([NOEYTRAL_PROBLEM_ID], [NOEYTRALT_PROBLEM_TIDSPUNKT]) før sammenligning.
 */
class EgenvurderingDialogJsonKontraktTest : FreeSpec({
    val periodeId = UUID.fromString("6d6302a7-7ed1-40a3-8257-c3e8ade4c049")
    val egenvurderingId = UUID.fromString("2656398c-a355-4f9b-8b34-a76abaf3c61a")
    val dialogPath = "/api/v1/egenvurdering/dialog"

    val testContext = TestContext.build()
    beforeSpec { testContext.mockOAuth2Server.start() }
    afterSpec { testContext.mockOAuth2Server.shutdown() }

    "Kontrakt for respons fra POST $dialogPath" - {
        val dialogServiceMock = mockk<DialogService>()
        with(testContext) {

            var mottatt: Any? = null

            fun ApplicationTestBuilder.testruter() {
                // Testruter som leser fasit med produksjonens ContentNegotiation-oppsett.
                routing {
                    post("/test/json-kontrakt/dialog-respons") {
                        mottatt = call.receive<EgenvurderingDialogResponse>()
                        call.respond(HttpStatusCode.NoContent)
                    }
                    post("/test/json-kontrakt/problem") {
                        mottatt = call.receive<ProblemDetails>()
                        call.respond(HttpStatusCode.NoContent)
                    }
                }
            }

            suspend fun ApplicationTestBuilder.lesMedProduksjonsmapper(sti: String, fasit: String): Any? {
                mottatt = null
                client.post(sti) {
                    contentType(ContentType.Application.Json)
                    setBody(fasit)
                }.status shouldBe HttpStatusCode.NoContent
                return mottatt
            }

            suspend fun ApplicationTestBuilder.postDialog(body: String = """{"periodeId":"$periodeId"}""") =
                client.post(dialogPath) {
                    contentType(ContentType.Application.Json)
                    bearerAuth(mockOAuth2Server.issueAzureAdToken().serialize())
                    setBody(body)
                }

            fun problem(type: java.net.URI, status: HttpStatusCode, detail: String) = ProblemDetails(
                id = UUID.fromString(NOEYTRAL_PROBLEM_ID),
                type = type,
                status = status,
                title = status.description,
                detail = detail,
                instance = dialogPath,
                timestamp = Instant.parse(NOEYTRALT_PROBLEM_TIDSPUNKT)
            )

            "dialog funnet skal serialiseres og deserialiseres likt fasit" {
                val sti = "egenvurdering-dialog/dialog-funnet.json"
                every { dialogServiceMock.finnDialogInfoForPeriodeId(periodeId) } returns PeriodeDialogRow(
                    periodeId = periodeId,
                    dialogId = 4141121L,
                    periodeDialogAuditRows = emptyList()
                )
                securedTestApplication(dialogServiceMock) {
                    testruter()
                    val response = postDialog()
                    response.status shouldBe HttpStatusCode.OK
                    response.bodyAsText() skalVaereLikFasit sti

                    lesMedProduksjonsmapper("/test/json-kontrakt/dialog-respons", lesFasit(sti)) shouldBe
                        EgenvurderingDialogResponse(dialogId = 4141121L)
                }
            }

            "dialog ikke funnet skal gi feilbody lik fasit" {
                val sti = "egenvurdering-dialog/feil-dialog-ikke-funnet.json"
                every { dialogServiceMock.finnDialogInfoForPeriodeId(periodeId) } returns null
                securedTestApplication(dialogServiceMock) {
                    testruter()
                    val response = postDialog()
                    response.status shouldBe HttpStatusCode.NotFound
                    noeytraliserProblemDetails(response.bodyAsText()) skalVaereLikFasit sti

                    lesMedProduksjonsmapper("/test/json-kontrakt/problem", lesFasit(sti)) shouldBe problem(
                        DIALOG_IKKE_FUNNET_ERROR_TYPE,
                        HttpStatusCode.NotFound,
                        "Dialog ikke funnet for arbeidssøkerperiode"
                    )
                }
            }

            "periode uten dialogId skal gi feilbody lik fasit" {
                val sti = "egenvurdering-dialog/feil-intern-feil.json"
                every { dialogServiceMock.finnDialogInfoForPeriodeId(periodeId) } returns PeriodeDialogRow(
                    periodeId = periodeId,
                    dialogId = null,
                    periodeDialogAuditRows = listOf(
                        PeriodeDialogAuditRow(
                            id = 1L,
                            periodeId = periodeId,
                            egenvurderingId = egenvurderingId,
                            dialogHttpStatusCode = 500,
                            dialogErrorMessage = "Feil"
                        )
                    )
                )
                securedTestApplication(dialogServiceMock) {
                    testruter()
                    val response = postDialog()
                    response.status shouldBe HttpStatusCode.InternalServerError
                    noeytraliserProblemDetails(response.bodyAsText()) skalVaereLikFasit sti

                    lesMedProduksjonsmapper("/test/json-kontrakt/problem", lesFasit(sti)) shouldBe problem(
                        EGENVURDERING_DIALOG_TJENESTE_INTERNAL_SERVER_ERROR,
                        HttpStatusCode.InternalServerError,
                        "Noe gikk galt ved henting av dialog for arbeidssøkerperiode"
                    )
                }
            }

            "ugyldig forespørsel skal gi feilbody lik fasit" {
                val sti = "egenvurdering-dialog/feil-ugyldig-json.json"
                securedTestApplication(dialogServiceMock) {
                    testruter()
                    val response = postDialog(body = """{"periodeId":"ikke-en-uuid"}""")
                    response.status shouldBe HttpStatusCode.BadRequest
                    noeytraliserProblemDetails(response.bodyAsText()) skalVaereLikFasit sti

                    lesMedProduksjonsmapper("/test/json-kontrakt/problem", lesFasit(sti)) shouldBe problem(
                        "kunne-ikke-tolke-forespoersel".asHttpErrorType(),
                        HttpStatusCode.BadRequest,
                        "Kunne ikke tolke forespørsel"
                    )
                }
            }
        }
    }

    "Kontrakt for utgående DialogRequest til veilarbdialog" - {
        listOf(
            Triple(
                "ny tråd med fnr",
                "egenvurdering-dialog/dialog-request-ny-traad.json",
                DialogRequest.nyTråd(
                    tekst = "Du har svart at du trenger veiledning.",
                    overskrift = "Egenvurdering",
                    fnr = "01017012345",
                    venterPaaSvarFraNav = true
                )
            ),
            Triple(
                "ny melding med fnr",
                "egenvurdering-dialog/dialog-request-ny-melding.json",
                DialogRequest.nyMelding(
                    tekst = "Du har svart at du klarer deg selv.",
                    dialogId = DialogId("4141121"),
                    fnr = "01017012345",
                    venterPaaSvarFraNav = false
                )
            ),
            Triple(
                "ny tråd uten fnr",
                "egenvurdering-dialog/dialog-request-ny-traad-uten-fnr.json",
                DialogRequest.nyTråd(
                    tekst = "Tekst",
                    overskrift = "Overskrift",
                    fnr = null,
                    venterPaaSvarFraNav = false
                )
            ),
            Triple(
                "ny melding uten fnr",
                "egenvurdering-dialog/dialog-request-ny-melding-uten-fnr.json",
                DialogRequest.nyMelding(
                    tekst = "Tekst",
                    dialogId = DialogId("4141121"),
                    fnr = null,
                    venterPaaSvarFraNav = true
                )
            )
        ).forEach { (navn, sti, eksempel) ->
            "$navn skal serialiseres og deserialiseres likt fasit" {
                var sendtBody: String? = null
                val motor = MockEngine { request ->
                    sendtBody = request.body.toByteArray().decodeToString()
                    respond(
                        content = """{"id":"4141121"}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    )
                }
                val texasClient = mockk<TexasClient>()
                coEvery { texasClient.getMachineToMachineToken(any()) } returns MachineToMachineTokenResponse("token")
                val veilarbdialogClient = VeilarbdialogClient(
                    config = VeilarbdialogClientConfig(url = "http://veilarbdialog", target = "veilarbdialog"),
                    texasClient = texasClient,
                    httpClient = createHttpClient(engineFactory = motor.somFactory())
                )

                veilarbdialogClient.lagEllerOppdaterDialog(eksempel)
                sendtBody!! skalVaereLikFasit sti

                // Deserialiser fasit med samme produksjonsklient (createHttpClient) som sender requesten.
                val leseklient = createHttpClient(engineFactory = MockEngine {
                    respond(
                        content = lesFasit(sti),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    )
                }.somFactory())
                leseklient.post("http://fasit").body<DialogRequest>() shouldBe eksempel
            }
        }
    }
})

/** Lar `createHttpClient(engineFactory = ...)` bruke en ferdig [MockEngine]. */
private fun MockEngine.somFactory(): HttpClientEngineFactory<MockEngineConfig> {
    val motor = this
    return object : HttpClientEngineFactory<MockEngineConfig> {
        override fun create(block: MockEngineConfig.() -> Unit): HttpClientEngine = motor
    }
}
