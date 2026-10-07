package no.nav.paw.ledigestillinger.kontrakt

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
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.every
import io.mockk.mockk
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.error.model.ProblemDetails
import no.nav.paw.error.model.asHttpErrorType
import no.nav.paw.error.plugin.installErrorHandlingPlugin
import no.nav.paw.health.healthChecksOf
import no.nav.paw.ledigestillinger.exception.MALFORMED_REQUEST_ERROR_TYPE
import no.nav.paw.ledigestillinger.exception.STILLING_IKKE_FUNNET_ERROR_TYPE
import no.nav.paw.ledigestillinger.exception.StillingIkkeFunnetException
import no.nav.paw.ledigestillinger.kontrakt.JsonKontrakt.NOEYTRALT_PROBLEM_TIDSPUNKT
import no.nav.paw.ledigestillinger.kontrakt.JsonKontrakt.NOEYTRAL_PROBLEM_ID
import no.nav.paw.ledigestillinger.kontrakt.JsonKontrakt.lesFasit
import no.nav.paw.ledigestillinger.kontrakt.JsonKontrakt.noeytraliserProblemDetails
import no.nav.paw.ledigestillinger.kontrakt.JsonKontrakt.skalVaereLikFasit
import no.nav.paw.ledigestillinger.plugin.configureRouting
import no.nav.paw.ledigestillinger.plugin.installWebPlugins
import no.nav.paw.ledigestillinger.service.StillingService
import no.nav.paw.ledigestillinger.test.TestData
import no.nav.paw.security.authentication.config.SECURITY_CONFIG
import no.nav.paw.security.authentication.config.SecurityConfig
import no.nav.paw.security.authentication.plugin.installAuthenticationPlugin
import no.nav.paw.serialization.plugin.installContentNegotiationPlugin
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.naw.paw.ledigestillinger.model.Arbeidsgiver
import no.naw.paw.ledigestillinger.model.FinnStillingerResponse
import no.naw.paw.ledigestillinger.model.Frist
import no.naw.paw.ledigestillinger.model.FristType
import no.naw.paw.ledigestillinger.model.Lokasjon
import no.naw.paw.ledigestillinger.model.PagingResponse
import no.naw.paw.ledigestillinger.model.Sektor
import no.naw.paw.ledigestillinger.model.SortOrder
import no.naw.paw.ledigestillinger.model.Stilling
import no.naw.paw.ledigestillinger.model.StillingStatus
import no.naw.paw.ledigestillinger.model.Stillingsprosent
import no.naw.paw.ledigestillinger.model.StyrkKode
import no.naw.paw.ledigestillinger.model.Tag
import no.naw.paw.ledigestillinger.model.TekniskTag
import no.naw.paw.ledigestillinger.model.VisningGrad
import java.net.URI
import java.time.Instant
import java.time.LocalDate
import java.util.*

/**
 * JSON-kontrakttest for ledigestillinger-api (`/api/v1/stillinger`).
 *
 * Appen settes opp med samme plugins som produksjon (`installWebPlugins()`,
 * `installContentNegotiationPlugin()`, `installErrorHandlingPlugin()` og `configureRouting`),
 * med mocket [StillingService] som returnerer faste data.
 * Fasitfilene i `src/test/resources/json-kontrakt/stillinger/` er generert fra produksjonskoden
 * og må aldri endres. Feiler testen, har kontrakten endret seg.
 *
 * ProblemDetails har tilfeldig `id` og `timestamp`. Disse erstattes med faste verdier
 * ([NOEYTRAL_PROBLEM_ID], [NOEYTRALT_PROBLEM_TIDSPUNKT]) før sammenligning.
 */
class StillingJsonKontraktTest : FreeSpec({
    val mockOAuth2Server = MockOAuth2Server()
    val securityConfig = loadNaisOrLocalConfiguration<SecurityConfig>(SECURITY_CONFIG)
    val stillingService = mockk<StillingService>()
    var mottatt: Any? = null

    beforeSpec { mockOAuth2Server.start() }
    afterSpec { mockOAuth2Server.shutdown() }

    fun ApplicationTestBuilder.kontraktApp() {
        application {
            installWebPlugins()
            installContentNegotiationPlugin()
            installErrorHandlingPlugin()
            installAuthenticationPlugin(securityConfig.authProviders.map {
                it.copy(
                    discoveryUrl = mockOAuth2Server.wellKnownUrl("default").toString(),
                    audiences = listOf("default")
                )
            })
            configureRouting(
                healthChecks = healthChecksOf(),
                meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
                stillingService = stillingService
            )
        }
        // Testruter som leser fasit med produksjonens ContentNegotiation-oppsett.
        routing {
            post("/test/json-kontrakt/stilling") {
                mottatt = call.receive<Stilling>()
                call.respond(HttpStatusCode.NoContent)
            }
            post("/test/json-kontrakt/finn-stillinger") {
                mottatt = call.receive<FinnStillingerResponse>()
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

    fun token() = mockOAuth2Server.issueToken(
        claims = mapOf("acr" to "idporten-loa-high", "pid" to TestData.fnr1)
    ).serialize()

    fun problem(type: URI, status: HttpStatusCode, detail: String, instance: String) = ProblemDetails(
        id = UUID.fromString(NOEYTRAL_PROBLEM_ID),
        type = type,
        status = status,
        title = status.description,
        detail = detail,
        instance = instance,
        timestamp = Instant.parse(NOEYTRALT_PROBLEM_TIDSPUNKT)
    )

    // Fullt utfylt stilling, alle Tag og TekniskTag.
    val fullStilling = Stilling(
        uuid = UUID.fromString("44b44747-f65d-46b3-89a8-997a63d0d489"),
        adnr = "1234567",
        tittel = "Utvikler",
        status = StillingStatus.AKTIV,
        visning = VisningGrad.UBEGRENSET,
        arbeidsgivernavn = "Testbedrift AS",
        arbeidsgiver = Arbeidsgiver(
            orgForm = "BEDR",
            navn = "TESTBEDRIFT AS",
            offentligNavn = "Testbedrift AS",
            orgNr = "999999999",
            parentOrgNr = "888888888"
        ),
        stillingstittel = "Systemutvikler",
        ansettelsesform = "Fast",
        stillingsprosent = Stillingsprosent.HELTID,
        stillingsantall = 2,
        sektor = Sektor.OFFENTLIG,
        soeknadsfrist = Frist(type = FristType.DATO, verdi = "2026-02-01", dato = LocalDate.parse("2026-02-01")),
        oppstartsfrist = Frist(type = FristType.SNAREST, verdi = "Snarest", dato = null),
        publisert = Instant.parse("2026-01-15T10:15:30.123456Z"),
        utloeper = Instant.parse("2026-02-01T22:59:59.999999Z"),
        styrkkoder = listOf(
            StyrkKode(kode = "2512", navn = "Programvareutviklere"),
            StyrkKode(kode = "2511", navn = "Systemanalytikere")
        ),
        lokasjoner = listOf(
            Lokasjon(
                land = "NORGE",
                postkode = "0661",
                poststed = "OSLO",
                kommune = "OSLO",
                kommunenummer = "0301",
                fylke = "OSLO",
                fylkesnummer = "03"
            )
        ),
        tags = Tag.entries.toList(),
        tekniskeTags = TekniskTag.entries.toList()
    )

    // Minimal stilling: alle nullbare felt er null og lister er tomme.
    val minimalStilling = Stilling(
        uuid = UUID.fromString("f0e09ebf-e9f7-4025-9bd7-31bbff037eaa"),
        adnr = null,
        tittel = "Minimal",
        status = StillingStatus.STOPPET,
        visning = null,
        arbeidsgivernavn = null,
        arbeidsgiver = null,
        stillingstittel = null,
        ansettelsesform = null,
        stillingsprosent = Stillingsprosent.UKJENT,
        stillingsantall = null,
        sektor = Sektor.UKJENT,
        soeknadsfrist = Frist(type = FristType.UKJENT, verdi = null, dato = null),
        oppstartsfrist = Frist(type = FristType.FORTLOEPENDE, verdi = null, dato = null),
        publisert = Instant.parse("2026-01-15T10:15:30.000001Z"),
        utloeper = null,
        styrkkoder = emptyList(),
        lokasjoner = emptyList(),
        tags = emptyList(),
        tekniskeTags = emptyList()
    )

    // Øvrige enum-verdier for status, visning, stillingsprosent og sektor.
    val oevrigeStillinger = listOf(
        Triple(StillingStatus.INAKTIV, VisningGrad.BEGRENSET_INTERNT, "e7b8c9f6-9ada-457c-bed5-ec45656c73b2"),
        Triple(StillingStatus.SLETTET, VisningGrad.BEGRENSET_ARBEIDSGIVER, "91d5e8cc-0edb-4378-ba71-39465e2ebfb8"),
        Triple(StillingStatus.AVVIST, VisningGrad.BEGRENSET_KILDE, "0bd29537-64e8-4e09-97da-886aa3a63103")
    ).map { (status, visning, uuid) ->
        minimalStilling.copy(
            uuid = UUID.fromString(uuid),
            tittel = "Stilling $status",
            status = status,
            visning = visning,
            stillingsprosent = Stillingsprosent.DELTID,
            sektor = Sektor.PRIVAT,
            lokasjoner = listOf(Lokasjon(land = "NORGE"))
        )
    }

    "Kontrakt for GET /api/v1/stillinger/{uuid}" - {
        listOf(
            Triple("fullt utfylt stilling", "stillinger/stilling-fullt-utfylt.json", fullStilling),
            Triple("minimal stilling", "stillinger/stilling-minimal.json", minimalStilling)
        ).forEach { (navn, sti, eksempel) ->
            "$navn skal serialiseres og deserialiseres likt fasit" {
                every { stillingService.hentStilling(eksempel.uuid) } returns eksempel
                testApplication {
                    kontraktApp()
                    val response = client.get("/api/v1/stillinger/${eksempel.uuid}") { bearerAuth(token()) }
                    response.status shouldBe HttpStatusCode.OK
                    response.bodyAsText() skalVaereLikFasit sti

                    lesMedProduksjonsmapper("/test/json-kontrakt/stilling", lesFasit(sti)) shouldBe eksempel
                }
            }
        }

        "stilling ikke funnet skal gi feilbody lik fasit" {
            val sti = "stillinger/feil-stilling-ikke-funnet.json"
            val uuid = UUID.fromString("2656398c-a355-4f9b-8b34-a76abaf3c61a")
            every { stillingService.hentStilling(uuid) } throws StillingIkkeFunnetException()
            testApplication {
                kontraktApp()
                val response = client.get("/api/v1/stillinger/$uuid") { bearerAuth(token()) }
                response.status shouldBe HttpStatusCode.NotFound
                noeytraliserProblemDetails(response.bodyAsText()) skalVaereLikFasit sti

                lesMedProduksjonsmapper("/test/json-kontrakt/problem", lesFasit(sti)) shouldBe problem(
                    STILLING_IKKE_FUNNET_ERROR_TYPE,
                    HttpStatusCode.NotFound,
                    "Stilling ikke funnet",
                    "/api/v1/stillinger/$uuid"
                )
            }
        }
    }

    "Kontrakt for POST /api/v1/stillinger" - {
        "søk på uuid-liste skal serialiseres og deserialiseres likt fasit" {
            val sti = "stillinger/finn-stillinger-by-uuid-liste.json"
            val stillinger = listOf(fullStilling, minimalStilling) + oevrigeStillinger
            every { stillingService.finnStillingerByUuidListe(any()) } returns stillinger
            testApplication {
                kontraktApp()
                val response = client.post("/api/v1/stillinger") {
                    bearerAuth(token())
                    contentType(ContentType.Application.Json)
                    setBody(
                        """{"type":"BY_UUID_LISTE","uuidListe":[${stillinger.joinToString(",") { "\"${it.uuid}\"" }}]}"""
                    )
                }
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() skalVaereLikFasit sti

                lesMedProduksjonsmapper("/test/json-kontrakt/finn-stillinger", lesFasit(sti)) shouldBe
                    FinnStillingerResponse(
                        stillinger = stillinger,
                        paging = PagingResponse(page = 1, pageSize = 5, hitSize = 5, sortOrder = SortOrder.DESC)
                    )
            }
        }

        "søk på egenskaper uten treff skal serialiseres og deserialiseres likt fasit" {
            val sti = "stillinger/finn-stillinger-by-egenskaper-tomt-resultat.json"
            every { stillingService.finnStillingerByEgenskaper(any(), any(), any(), any(), any()) } returns emptyList()
            testApplication {
                kontraktApp()
                val response = client.post("/api/v1/stillinger") {
                    bearerAuth(token())
                    contentType(ContentType.Application.Json)
                    setBody(
                        """{"type":"BY_EGENSKAPER","soekeord":[],"styrkkoder":[],"fylker":[],"paging":{"page":2,"pageSize":20,"sortOrder":"ASC"}}"""
                    )
                }
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() skalVaereLikFasit sti

                lesMedProduksjonsmapper("/test/json-kontrakt/finn-stillinger", lesFasit(sti)) shouldBe
                    FinnStillingerResponse(
                        stillinger = emptyList(),
                        paging = PagingResponse(page = 2, pageSize = 20, hitSize = 0, sortOrder = SortOrder.ASC)
                    )
            }
        }

        "ugyldig paging skal gi feilbody lik fasit" {
            val sti = "stillinger/feil-ugyldig-paging.json"
            testApplication {
                kontraktApp()
                val response = client.post("/api/v1/stillinger") {
                    bearerAuth(token())
                    contentType(ContentType.Application.Json)
                    setBody(
                        """{"type":"BY_EGENSKAPER","soekeord":[],"styrkkoder":[],"fylker":[],"paging":{"page":0,"pageSize":10,"sortOrder":"DESC"}}"""
                    )
                }
                response.status shouldBe HttpStatusCode.BadRequest
                noeytraliserProblemDetails(response.bodyAsText()) skalVaereLikFasit sti

                lesMedProduksjonsmapper("/test/json-kontrakt/problem", lesFasit(sti)) shouldBe problem(
                    MALFORMED_REQUEST_ERROR_TYPE,
                    HttpStatusCode.BadRequest,
                    "Page må være et positivt tall",
                    "/api/v1/stillinger"
                )
            }
        }

        "ugyldig JSON skal gi feilbody lik fasit" {
            val sti = "stillinger/feil-ugyldig-json.json"
            testApplication {
                kontraktApp()
                val response = client.post("/api/v1/stillinger") {
                    bearerAuth(token())
                    contentType(ContentType.Application.Json)
                    setBody("""{"type":"BY_UUID_LISTE","uuidListe":["ikke-en-uuid"]}""")
                }
                response.status shouldBe HttpStatusCode.BadRequest
                noeytraliserProblemDetails(response.bodyAsText()) skalVaereLikFasit sti

                lesMedProduksjonsmapper("/test/json-kontrakt/problem", lesFasit(sti)) shouldBe problem(
                    "kunne-ikke-tolke-forespoersel".asHttpErrorType(),
                    HttpStatusCode.BadRequest,
                    "Kunne ikke tolke forespørsel",
                    "/api/v1/stillinger"
                )
            }
        }
    }
})
