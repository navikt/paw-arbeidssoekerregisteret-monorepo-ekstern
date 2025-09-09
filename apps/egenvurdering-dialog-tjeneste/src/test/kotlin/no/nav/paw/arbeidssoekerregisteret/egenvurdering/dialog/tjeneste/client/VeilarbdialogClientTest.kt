package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.client

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.jackson
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.config.VeilarbdialogClientConfig
import no.nav.paw.client.factory.configureJackson
import no.nav.paw.security.texas.TexasClient
import no.nav.paw.security.texas.m2m.MachineToMachineTokenResponse

class VeilarbdialogClientTest : FreeSpec({
    val dialogTestEndepunkt = "http://veilarbdialog.dab/veilarbdialog"
    val testConfig = VeilarbdialogClientConfig(
        url = dialogTestEndepunkt,
        scope = "dialog-test-scope",
    )

    val token = "test-m2m-token"

    "Får postet ny tråd og melding til dialog" - {
        val mockEngine = MockEngine { request ->
            request.method shouldBe HttpMethod.Post
            request.url shouldBe Url("$dialogTestEndepunkt$veilarbDialogPath")
            request.headers[HttpHeaders.Authorization] shouldBe "Bearer $token"

            respond(
                content = dialogResponseJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val texasClientMock = mockk<TexasClient>()
        coEvery { texasClientMock.getMachineToMachineToken() } returns MachineToMachineTokenResponse(accessToken = token)

        val veilarbDialogClient = VeilarbdialogClient(
            config = testConfig,
            texasClient = texasClientMock,
            httpClient = testClient(mockEngine)
        )

        runBlocking {
            veilarbDialogClient.lagEllerOppdaterDialog(
                DialogRequest.nyTråd(
                    tekst = "tekst",
                    overskrift = "overskrift",
                    venterPaaSvarFraNav = true
                )
            )
        }.shouldBeInstanceOf<DialogResponse> { resultat ->
            resultat.dialogId shouldBe testDialogId.value
        }

        runBlocking {
            veilarbDialogClient.lagEllerOppdaterDialog(
                DialogRequest.nyMelding(
                    tekst = "tekst",
                    dialogId = testDialogId,
                    venterPaaSvarFraNav = false
                )
            )
        }.shouldBeInstanceOf<DialogResponse> { resultat ->
            resultat.dialogId shouldBe testDialogId.value
        }
    }

    "Arbeidsoppfølgingsperiode er avsluttet" - {
        val mockEngine = MockEngine { request ->
            request.method shouldBe HttpMethod.Post
            request.url shouldBe Url("$dialogTestEndepunkt$veilarbDialogPath")
            request.headers[HttpHeaders.Authorization] shouldBe "Bearer $token"

            respond(
                content = "Funksjonell feil under behandling: NyHenvendelsePåHistoriskDialogException - Kan ikke sende henvendelse på historisk dialog ",
                status = HttpStatusCode.Conflict,
            )
        }

        val texasClientMock = mockk<TexasClient>()
        coEvery { texasClientMock.getMachineToMachineToken() } returns MachineToMachineTokenResponse(token)

        val veilarbDialogClient = VeilarbdialogClient(
            config = testConfig,
            texasClient = texasClientMock,
            httpClient = testClient(mockEngine)
        )

        runBlocking {
            veilarbDialogClient.lagEllerOppdaterDialog(
                DialogRequest.nyMelding(
                    tekst = "tekst",
                    dialogId = DialogId("adsasd"),
                    venterPaaSvarFraNav = true
                )
            )
        }.shouldBeInstanceOf<ArbeidsoppfølgingsperiodeAvsluttet>()
    }

    "Feilsituasjon kaster VeilarbdialogClientException" - {
        val mockEngine = MockEngine { request ->
            request.method shouldBe HttpMethod.Post
            request.url shouldBe Url("$dialogTestEndepunkt$veilarbDialogPath")
            request.headers[HttpHeaders.Authorization] shouldBe "Bearer $token"

            respond(
                content = "En eller annen feil",
                status = HttpStatusCode.Conflict,
            )
        }

        val texasClientMock = mockk<TexasClient>()
        coEvery { texasClientMock.getMachineToMachineToken() } returns MachineToMachineTokenResponse(token)

        val veilarbDialogClient = VeilarbdialogClient(
            config = testConfig,
            texasClient = texasClientMock,
            httpClient = testClient(mockEngine)
        )

        shouldThrow<VeilarbdialogClientException> {
            runBlocking {
                veilarbDialogClient.lagEllerOppdaterDialog(
                    DialogRequest.nyTråd(
                        tekst = "tekst",
                        overskrift = "overskrift",
                        venterPaaSvarFraNav = true
                    )
                )
            }
        }
    }
})

val testDialogId = DialogId("4141121")

//language=JSON
val dialogResponseJson = """
    {
      "id": "${testDialogId.value}",
      "aktivitetId": "string",
      "overskrift": "string",
      "sisteTekst": "string",
      "sisteDato": "2025-07-04T08:51:42.036Z",
      "opprettetDato": "2025-07-04T08:51:42.036Z",
      "historisk": false,
      "lest": true,
      "venterPaSvar": true,
      "ferdigBehandlet": true,
      "lestAvBrukerTidspunkt": "2025-07-04T08:51:42.036Z",
      "erLestAvBruker": true,
      "oppfolgingsperiode": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "henvendelser": [
        {
          "id": "string",
          "dialogId": "string",
          "avsender": "BRUKER",
          "avsenderId": "string",
          "sendt": "2025-07-04T08:51:42.036Z",
          "lest": true,
          "viktig": true,
          "tekst": "string"
        }
      ],
      "egenskaper": [
        "ESKALERINGSVARSEL"
      ]
    }
""".trimIndent()

private fun testClient(mockEngine: MockEngine): HttpClient = HttpClient(mockEngine) {
    install(ContentNegotiation) {
        jackson {
            configureJackson()
        }
    }
}
