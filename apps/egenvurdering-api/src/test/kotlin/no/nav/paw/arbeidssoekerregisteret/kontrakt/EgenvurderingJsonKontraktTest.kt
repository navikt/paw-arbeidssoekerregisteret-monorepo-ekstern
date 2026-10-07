package no.nav.paw.arbeidssoekerregisteret.kontrakt

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import no.nav.paw.arbeidssoekerregisteret.exception.EGENVURDERING_IKKE_STOETTET_ERROR_TYPE
import no.nav.paw.arbeidssoekerregisteret.exception.EgenvurderingIkkeStoettetException
import no.nav.paw.arbeidssoekerregisteret.kontrakt.JsonKontrakt.NOEYTRALT_PROBLEM_TIDSPUNKT
import no.nav.paw.arbeidssoekerregisteret.kontrakt.JsonKontrakt.NOEYTRAL_PROBLEM_ID
import no.nav.paw.arbeidssoekerregisteret.kontrakt.JsonKontrakt.lesFasit
import no.nav.paw.arbeidssoekerregisteret.kontrakt.JsonKontrakt.noeytraliserProblemDetails
import no.nav.paw.arbeidssoekerregisteret.kontrakt.JsonKontrakt.skalVaereLikFasit
import no.nav.paw.arbeidssoekerregisteret.model.EgenvurderingGrunnlag
import no.nav.paw.arbeidssoekerregisteret.model.Profilering
import no.nav.paw.arbeidssoekerregisteret.model.ProfilertTil
import no.nav.paw.arbeidssoekerregisteret.route.egenvurderingGrunnlagPath
import no.nav.paw.arbeidssoekerregisteret.route.egenvurderingPath
import no.nav.paw.arbeidssoekerregisteret.test.TestContext
import no.nav.paw.error.model.ProblemDetails
import no.nav.paw.error.model.asHttpErrorType
import java.time.Instant
import java.util.*
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil as AvroProfilertTil

/**
 * JSON-kontrakttest for egenvurdering-api.
 *
 * Responsene går gjennom samme plugin-oppsett som produksjon (`installContentNegotiationPlugin`
 * med `configureJacksonOverrides()` og `installErrorHandlingPlugin()`), via [TestContext].
 * Fasitfilene i `src/test/resources/json-kontrakt/egenvurdering/` er generert fra produksjonskoden
 * og må aldri endres. Feiler testen, har kontrakten endret seg.
 *
 * ProblemDetails har tilfeldig `id` og `timestamp`. Disse erstattes med faste verdier
 * ([NOEYTRAL_PROBLEM_ID], [NOEYTRALT_PROBLEM_TIDSPUNKT]) før sammenligning.
 */
class EgenvurderingJsonKontraktTest : FreeSpec({
    with(TestContext()) {
        beforeSpec { mockOAuth2Server.start() }
        afterSpec { mockOAuth2Server.shutdown() }

        var mottatt: Any? = null

        fun ApplicationTestBuilder.kontraktApp() {
            configureTestApplication()
            // Testruter som leser fasit med produksjonens ContentNegotiation-oppsett.
            routing {
                post("/test/json-kontrakt/grunnlag") {
                    mottatt = call.receive<EgenvurderingGrunnlag>()
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

        "Kontrakt for GET egenvurdering/grunnlag" - {
            val eksempler = ProfilertTil.entries.map { profilertTil ->
                "grunnlag med profilertTil $profilertTil" to (
                    "egenvurdering/grunnlag-profilert-til-${profilertTil.name.lowercase().replace('_', '-')}.json" to
                        EgenvurderingGrunnlag(
                            grunnlag = Profilering(
                                profileringId = UUID.fromString("44b44747-f65d-46b3-89a8-997a63d0d489"),
                                profilertTil = profilertTil
                            )
                        )
                    )
            } + ("uten grunnlag" to ("egenvurdering/grunnlag-uten-grunnlag.json" to EgenvurderingGrunnlag(grunnlag = null)))

            eksempler.forEach { (navn, stiOgEksempel) ->
                val (sti, eksempel) = stiOgEksempel
                "$navn skal serialiseres og deserialiseres likt fasit" {
                    coEvery { egenvurderingService.getEgenvurderingGrunnlag(any()) } returns eksempel
                    testApplication {
                        kontraktApp()
                        val response = client.get(egenvurderingGrunnlagPath) {
                            bearerAuth(mockOAuth2Server.issueTokenXToken().serialize())
                        }
                        response.status shouldBe HttpStatusCode.OK
                        response.bodyAsText() skalVaereLikFasit sti

                        lesMedProduksjonsmapper("/test/json-kontrakt/grunnlag", lesFasit(sti)) shouldBe eksempel
                    }
                }
            }
        }

        "Kontrakt for feilresponser" - {
            "ugyldig JSON i forespørsel skal gi feilbody lik fasit" {
                val sti = "egenvurdering/feil-ugyldig-json.json"
                testApplication {
                    kontraktApp()
                    val response = client.post(egenvurderingPath) {
                        contentType(ContentType.Application.Json)
                        bearerAuth(mockOAuth2Server.issueTokenXToken().serialize())
                        setBody("""{"profileringId":"ikke-en-uuid"}""")
                    }
                    response.status shouldBe HttpStatusCode.BadRequest
                    noeytraliserProblemDetails(response.bodyAsText()) skalVaereLikFasit sti

                    lesMedProduksjonsmapper("/test/json-kontrakt/problem", lesFasit(sti)) shouldBe ProblemDetails(
                        id = UUID.fromString(NOEYTRAL_PROBLEM_ID),
                        type = "kunne-ikke-tolke-forespoersel".asHttpErrorType(),
                        status = HttpStatusCode.BadRequest,
                        title = HttpStatusCode.BadRequest.description,
                        detail = "Kunne ikke tolke forespørsel",
                        instance = egenvurderingPath,
                        timestamp = Instant.parse(NOEYTRALT_PROBLEM_TIDSPUNKT)
                    )
                }
            }

            "egenvurdering ikke støttet skal gi feilbody lik fasit" {
                val sti = "egenvurdering/feil-egenvurdering-ikke-stoettet.json"
                coEvery { egenvurderingService.publiserOgLagreEgenvurdering(any(), any(), any()) } throws
                    EgenvurderingIkkeStoettetException(
                        profilering = AvroProfilertTil.ANTATT_GODE_MULIGHETER,
                        egenvurdering = AvroProfilertTil.OPPGITT_HINDRINGER
                    )
                testApplication {
                    kontraktApp()
                    val response = client.post(egenvurderingPath) {
                        contentType(ContentType.Application.Json)
                        bearerAuth(mockOAuth2Server.issueTokenXToken().serialize())
                        setBody("""{"profileringId":"44b44747-f65d-46b3-89a8-997a63d0d489","egenvurdering":"OPPGITT_HINDRINGER"}""")
                    }
                    response.status shouldBe HttpStatusCode.BadRequest
                    noeytraliserProblemDetails(response.bodyAsText()) skalVaereLikFasit sti

                    lesMedProduksjonsmapper("/test/json-kontrakt/problem", lesFasit(sti)) shouldBe ProblemDetails(
                        id = UUID.fromString(NOEYTRAL_PROBLEM_ID),
                        type = EGENVURDERING_IKKE_STOETTET_ERROR_TYPE,
                        status = HttpStatusCode.BadRequest,
                        title = HttpStatusCode.BadRequest.description,
                        detail = "Egenvurdering OPPGITT_HINDRINGER er ikke støttet for profilering ANTATT_GODE_MULIGHETER",
                        instance = egenvurderingPath,
                        timestamp = Instant.parse(NOEYTRALT_PROBLEM_TIDSPUNKT)
                    )
                }
            }
        }
    }
})
