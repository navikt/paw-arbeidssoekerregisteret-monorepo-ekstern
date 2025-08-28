package no.nav.paw.arbeidssoekerregisteret.client

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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
import kotlinx.coroutines.runBlocking
import no.nav.paw.arbeidssoekerregisteret.config.VeilarbdialogClientConfig
import no.nav.paw.client.factory.configureJackson

class DialogClientTest : FreeSpec({
    val dialogTestEndepunkt = "http://veilarbdialog.dab/veilarbdialog"
    val testConfig = VeilarbdialogClientConfig(
        url = dialogTestEndepunkt,
        scope = "dialog-test-scope",
    )

    "Får postet egenvurdering til dialog" - {
        val mockEngine = MockEngine { request ->
            request.method shouldBe HttpMethod.Post
            request.url shouldBe Url("$dialogTestEndepunkt$veilarbDialogPostPath")

            respond(
                content = dialogResponseJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val veilarbDialogClient = VeilarbdialogClient(config = testConfig, httpClient = testClient(mockEngine))

        val resultat = runBlocking {
            veilarbDialogClient.lagEllerOppdaterDialog(dialogRequestJson.tilDialogRequest())
        }
        (resultat as DialogResponse).dialogId shouldBe testDialogId
    }

    "Arbeidsoppfølgingsperiode er avsluttet" - {
        val mockEngine = MockEngine { request ->
            request.method shouldBe HttpMethod.Post
            request.url shouldBe Url("$dialogTestEndepunkt$veilarbDialogPostPath")

            respond(
                content = "Funksjonell feil under behandling: NyHenvendelsePåHistoriskDialogException - Kan ikke sende henvendelse på historisk dialog ",
                status = HttpStatusCode.Conflict,
            )
        }

        val veilarbDialogClient = VeilarbdialogClient(config = testConfig, httpClient = testClient(mockEngine))

        runBlocking {
            veilarbDialogClient.lagEllerOppdaterDialog(dialogRequestJson.tilDialogRequest())
        }.shouldBeInstanceOf<ArbeidsoppfølgingsperiodeAvsluttet>()
    }

    "Feilsituasjon kaster VeilarbdialogClientException" - {
        val mockEngine = MockEngine { request ->
            request.method shouldBe HttpMethod.Post
            request.url shouldBe Url("$dialogTestEndepunkt$veilarbDialogPostPath")

            respond(
                content = "En eller annen feil",
                status = HttpStatusCode.Conflict,
            )
        }
        val veilarbDialogClient = VeilarbdialogClient(config = testConfig, httpClient = testClient(mockEngine))
        shouldThrow<VeilarbdialogClientException> {
            veilarbDialogClient.lagEllerOppdaterDialog(dialogRequestJson.tilDialogRequest())
        }
    }
})

val testDialogId = "4141121"

//language=JSON
val dialogRequestJson = """
        {
          "tekst": "Egenvurdering tekst.",
          "dialogId": "$testDialogId",
          "overskrift": "Oppfølging av søknad",
          "aktivitetId": "4141121",
          "venterPaaSvarFraNav": true,
          "venterPaaSvarFraBruker": true,
          "egenskaper": [
            "ESKALERINGSVARSEL"
          ],
          "fnr": "string"
        }
    """.trimIndent()

fun String.tilDialogRequest(): DialogRequest {
    val mapper = jacksonObjectMapper()
    return mapper.readValue(this)
}

//language=JSON
val dialogResponseJson = """
    {
      "id": "$testDialogId",
      "aktivitetId": "string",
      "overskrift": "string",
      "sisteTekst": "string",
      "sisteDato": "2025-07-04T08:51:42.036Z",
      "opprettetDato": "2025-07-04T08:51:42.036Z",
      "historisk": true,
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