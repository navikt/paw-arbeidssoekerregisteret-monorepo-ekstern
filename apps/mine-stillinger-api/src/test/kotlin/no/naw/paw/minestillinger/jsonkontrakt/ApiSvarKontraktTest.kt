package no.naw.paw.minestillinger.jsonkontrakt

import tools.jackson.module.kotlin.readValue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.testing.testApplication
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.error.model.Data
import no.nav.paw.error.model.ProblemDetails
import no.nav.paw.serialization.jackson.buildObjectMapper
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.naw.paw.minestillinger.api.MineStillingerResponse
import no.naw.paw.minestillinger.api.vo.ApiBrukerprofil
import no.naw.paw.minestillinger.api.vo.ApiFylke
import no.naw.paw.minestillinger.api.vo.StyrkTreNode
import no.naw.paw.minestillinger.api.vo.populerFylkerMedKommuner
import no.naw.paw.minestillinger.api.vo.styrkTre
import no.naw.paw.minestillinger.brukerIkkeFunnet
import no.naw.paw.minestillinger.configureKtorServer
import no.naw.paw.minestillinger.kodeverk.SSBKodeverk
import no.naw.paw.minestillinger.oppdateringIkkeTillatt
import no.naw.paw.minestillinger.route.kodeverk
import no.naw.paw.minestillinger.route.respondWith
import no.naw.paw.minestillinger.tjenesteIkkeAktiv
import no.naw.paw.minestillinger.tokenXAuthProvider
import no.naw.paw.minestillinger.ugyldigVerdiIRequest
import java.time.Instant
import java.util.UUID

/**
 * JSON-kontrakt for HTTP-svar fra mine-stillinger-api.
 *
 * Svarene går gjennom ekte [configureKtorServer] (samme content negotiation som i produksjon)
 * og ekte [respondWith]/`call.respond`. BrukerprofilRoute og LedigeStillingerRoute trenger database,
 * så testen bruker egne testruter som svarer med de samme typene på samme måte som produksjonsrutene.
 * KodeverkRoute brukes direkte. Fasiten for kodeverk låser også SSB-dataene i ressursfilene.
 *
 * Deserialisering bruker [buildObjectMapper], som har samme oppsett som serverens content negotiation.
 *
 * ProblemDetails har tilfeldig `id` og `timestamp`. Svar fra feilmeldingsfabrikkene får faste verdier
 * via `copy`. I tillegg sjekkes ett ufiksert svar der `id` og `timestamp` valideres som UUID og
 * Instant og erstattes med plassholderne `<id>` og `<timestamp>` før sammenligning.
 *
 * Fasitfilene må aldri endres.
 */
class ApiSvarKontraktTest : FreeSpec({
    val oauthServer = MockOAuth2Server()
    beforeSpec { oauthServer.start() }
    afterSpec { oauthServer.shutdown() }
    val mapper = buildObjectMapper

    val brukerprofiler = mapOf(
        "api/brukerprofil-aktiv-med-alle-soektyper.json" to Eksempeldata.brukerprofilAktiv,
        "api/brukerprofil-inaktiv-tomme-lister.json" to Eksempeldata.brukerprofilInaktivTom,
        "api/brukerprofil-opt-out-tomme-soek.json" to Eksempeldata.brukerprofilOptOut,
        "api/brukerprofil-kan-ikke-leveres.json" to Eksempeldata.brukerprofilKanIkkeLeveres,
    )
    val mineStillinger = mapOf(
        "api/mine-stillinger-full.json" to Eksempeldata.mineStillingerFull,
        "api/mine-stillinger-tomt-resultat.json" to Eksempeldata.mineStillingerTom,
        "api/mine-stillinger-reisevei-soek.json" to Eksempeldata.mineStillingerReisevei,
    )
    val fastId = UUID.fromString("99999999-8888-7777-6666-555555555555")
    val fastTid = Instant.parse("2026-01-15T10:15:30.123456Z")
    val problemer = mapOf(
        "api/problemdetails-bruker-ikke-funnet.json" to brukerIkkeFunnet(),
        "api/problemdetails-tjeneste-ikke-aktiv.json" to tjenesteIkkeAktiv(),
        "api/problemdetails-ugyldig-verdi.json" to ugyldigVerdiIRequest("tjenestestatus", "UKJENT"),
        "api/problemdetails-oppdatering-ikke-tillatt.json" to oppdateringIkkeTillatt("Ikke lov"),
    ).mapValues { (_, p) -> p.copy(id = fastId, timestamp = fastTid) }

    suspend fun hentFraServer(sti: String): Pair<HttpStatusCode, String> {
        var resultat: Pair<HttpStatusCode, String>? = null
        testApplication {
            application {
                configureKtorServer(
                    prometheusRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
                    meterBinders = emptyList(),
                    authProviders = listOf(oauthServer.tokenXAuthProvider)
                )
            }
            routing {
                kodeverk()
                get("/kontrakt/brukerprofil") {
                    call.respondWith(Data(brukerprofiler.getValue(call.request.queryParameters["fil"]!!)))
                }
                get("/kontrakt/ledigestillinger") {
                    call.respond(mineStillinger.getValue(call.request.queryParameters["fil"]!!))
                }
                get("/kontrakt/problem") {
                    call.respondWith(problemer.getValue(call.request.queryParameters["fil"]!!))
                }
                get("/kontrakt/problem-ufiksert") {
                    call.respondWith(brukerIkkeFunnet())
                }
            }
            val response = client.get(sti)
            resultat = response.status to response.bodyAsText()
        }
        return resultat!!
    }

    "BrukerprofilRoute: ApiBrukerprofil" - {
        brukerprofiler.forEach { (fil, profil) ->
            "$fil" - {
                "svaret er likt fasitfilen" {
                    val (status, body) = hentFraServer("/kontrakt/brukerprofil?fil=$fil")
                    status shouldBe HttpStatusCode.OK
                    body skalVaereLikFasit fil
                }
                "fasitfilen leses til samme objekt" {
                    mapper.readValue<ApiBrukerprofil>(JsonKontrakt.lesFasit(fil)) shouldBe profil
                }
            }
        }
    }

    "LedigeStillingerRoute: MineStillingerResponse" - {
        mineStillinger.forEach { (fil, svar) ->
            "$fil" - {
                "svaret er likt fasitfilen" {
                    val (status, body) = hentFraServer("/kontrakt/ledigestillinger?fil=$fil")
                    status shouldBe HttpStatusCode.OK
                    body skalVaereLikFasit fil
                }
                "fasitfilen leses til samme objekt" {
                    mapper.readValue<MineStillingerResponse>(JsonKontrakt.lesFasit(fil)) shouldBe svar
                }
            }
        }
    }

    "KodeverkRoute" - {
        "fylker er likt fasitfilen og kan leses tilbake" {
            val fil = "api/kodeverk-fylker.json"
            val (status, body) = hentFraServer("/api/v1/kodeverk/fylker")
            status shouldBe HttpStatusCode.OK
            body skalVaereLikFasit fil
            mapper.readValue<List<ApiFylke>>(JsonKontrakt.lesFasit(fil)) shouldBe
                    populerFylkerMedKommuner(SSBKodeverk.fylker, SSBKodeverk.kommuner)
        }
        "styrk08 er likt fasitfilen og kan leses tilbake" {
            val fil = "api/kodeverk-styrk08.json"
            val (status, body) = hentFraServer("/api/v1/kodeverk/styrk08")
            status shouldBe HttpStatusCode.OK
            body skalVaereLikFasit fil
            mapper.readValue<List<StyrkTreNode>>(JsonKontrakt.lesFasit(fil)) shouldBe
                    SSBKodeverk.styrkKoder.styrkTre()
        }
    }

    "ProblemDetails" - {
        problemer.forEach { (fil, problem) ->
            "$fil" - {
                "svaret er likt fasitfilen" {
                    val (status, body) = hentFraServer("/kontrakt/problem?fil=$fil")
                    status shouldBe problem.status
                    body skalVaereLikFasit fil
                }
                "fasitfilen leses til samme objekt" {
                    mapper.readValue<ProblemDetails>(JsonKontrakt.lesFasit(fil)) shouldBe problem
                }
            }
        }
        "ufiksert svar med nøytralisert id og timestamp er likt fasitfilen" {
            val (status, body) = hentFraServer("/kontrakt/problem-ufiksert")
            status shouldBe HttpStatusCode.NotFound
            noeytraliser(body) skalVaereLikFasit "api/problemdetails-bruker-ikke-funnet-noeytralisert.json"
        }
    }
})

/**
 * Validerer at `id` er en UUID og `timestamp` en ISO-8601 Instant, og erstatter verdiene
 * med plassholdere. Feltrekkefølgen beholdes.
 */
fun noeytraliser(json: String): String {
    val mapper = buildObjectMapper
    val node = mapper.readTree(json) as tools.jackson.databind.node.ObjectNode
    UUID.fromString(node.get("id").asText())
    Instant.parse(node.get("timestamp").asText())
    node.put("id", "<id>")
    node.put("timestamp", "<timestamp>")
    return mapper.writeValueAsString(node)
}
