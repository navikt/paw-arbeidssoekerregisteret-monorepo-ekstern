package no.nav.paw.arbeidssoekerregisteret.eksternt.api.kontrakt

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.every
import io.mockk.mockk
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.kontrakt.JsonKontrakt.NOEYTRALT_PROBLEM_TIDSPUNKT
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.kontrakt.JsonKontrakt.NOEYTRAL_PROBLEM_ID
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.kontrakt.JsonKontrakt.lesFasit
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.kontrakt.JsonKontrakt.noeytraliserProblemDetails
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.kontrakt.JsonKontrakt.skalVaereLikFasit
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.models.ArbeidssoekerperiodeResponse
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.plugins.configureHTTP
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.plugins.configureRouting
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.plugins.configureSerialization
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.services.PeriodeService
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.test.ApplicationTestContext
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.test.TestData
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.test.configureMockAuthentication
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.test.issueMaskinportenToken
import no.nav.paw.error.model.ProblemDetails
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import java.net.URI
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

/**
 * JSON-kontrakttest for `POST /api/v1/arbeidssoekerperioder`, som brukes av eksterne partnere.
 *
 * Responsen serialiseres gjennom produksjonens `configureSerialization()` og `configureHTTP()`
 * (ErrorHandlingPlugin). Fasitfilene i `src/test/resources/json-kontrakt/arbeidssoekerperioder/`
 * er generert fra produksjonskoden og må aldri endres. Feiler testen, har kontrakten endret seg.
 *
 * ProblemDetails har tilfeldig `id` og `timestamp`. Disse erstattes med faste verdier
 * ([NOEYTRAL_PROBLEM_ID], [NOEYTRALT_PROBLEM_TIDSPUNKT]) før sammenligning; se
 * [JsonKontrakt.noeytraliserProblemDetails].
 */
class ArbeidssoekerperiodeJsonKontraktTest : FreeSpec({
    val testContext = ApplicationTestContext.withMockDataAccess()
    val periodeService: PeriodeService = mockk()
    var mottatt: Any? = null

    fun Application.kontraktApp() {
        configureHTTP()
        configureMockAuthentication(testContext.securityConfig, testContext.mockOAuth2Server)
        configureSerialization()
        configureRouting(mockk<PrometheusMeterRegistry>(relaxed = true), periodeService)
        // Testrute som leser fasit med produksjonens ContentNegotiation-oppsett.
        routing {
            post("/test/json-kontrakt/perioder") {
                mottatt = call.receive<List<ArbeidssoekerperiodeResponse>>()
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

    beforeSpec { testContext.mockOAuth2Server.start() }
    afterSpec { testContext.mockOAuth2Server.shutdown() }

    val avsluttetPeriode = ArbeidssoekerperiodeResponse(
        periodeId = UUID.fromString("6d6302a7-7ed1-40a3-8257-c3e8ade4c049"),
        startet = LocalDateTime.parse("2026-01-15T10:15:30.123"),
        avsluttet = LocalDateTime.parse("2026-02-20T08:05:01.456")
    )
    val aapenPeriode = ArbeidssoekerperiodeResponse(
        periodeId = UUID.fromString("2656398c-a355-4f9b-8b34-a76abaf3c61a"),
        startet = LocalDateTime.parse("2026-03-01T00:00:00.001"),
        avsluttet = null
    )

    "Kontrakt for arbeidssøkerperioder" - {
        listOf(
            Triple("liste med avsluttet og åpen periode", "arbeidssoekerperioder/perioder-avsluttet-og-aapen.json", listOf(avsluttetPeriode, aapenPeriode)),
            Triple("tom liste", "arbeidssoekerperioder/perioder-tom-liste.json", emptyList())
        ).forEach { (navn, sti, eksempel) ->
            "$navn skal serialiseres og deserialiseres likt fasit" {
                every { periodeService.hentPerioder(any(), any()) } returns eksempel
                testApplication {
                    application { kontraktApp() }
                    val response = client.post("/api/v1/arbeidssoekerperioder") {
                        contentType(ContentType.Application.Json)
                        bearerAuth(testContext.mockOAuth2Server.issueMaskinportenToken())
                        setBody("""{"identitetsnummer":"${TestData.fnr1}"}""")
                    }
                    response.status shouldBe HttpStatusCode.OK
                    response.bodyAsText() skalVaereLikFasit sti

                    lesMedProduksjonsmapper("/test/json-kontrakt/perioder", lesFasit(sti)) shouldBe eksempel
                }
            }
        }

        "feilrespons ved ugyldig dato skal serialiseres og deserialiseres likt fasit" {
            val sti = "arbeidssoekerperioder/feil-ugyldig-fra-startet-dato.json"
            testApplication {
                application { kontraktApp() }
                val response = client.post("/api/v1/arbeidssoekerperioder") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(testContext.mockOAuth2Server.issueMaskinportenToken())
                    setBody("""{"identitetsnummer":"${TestData.fnr1}","fraStartetDato":"01-01-2021"}""")
                }
                response.status shouldBe BadRequest
                noeytraliserProblemDetails(response.bodyAsText()) skalVaereLikFasit sti

                lesMedProduksjonsmapper("/test/json-kontrakt/problem", lesFasit(sti)) shouldBe ProblemDetails(
                    id = UUID.fromString(NOEYTRAL_PROBLEM_ID),
                    type = URI.create("urn:paw:http:kunne-ikke-tolke-forespoersel"),
                    status = BadRequest,
                    title = BadRequest.description,
                    detail = "Kunne ikke tolke forespørsel",
                    instance = "/api/v1/arbeidssoekerperioder",
                    timestamp = Instant.parse(NOEYTRALT_PROBLEM_TIDSPUNKT)
                )
            }
        }
    }
})
