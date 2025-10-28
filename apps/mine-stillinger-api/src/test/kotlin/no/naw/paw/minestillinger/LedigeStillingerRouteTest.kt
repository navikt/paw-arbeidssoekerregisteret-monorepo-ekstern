package no.naw.paw.minestillinger

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEqualIgnoringCase
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.ContentType.Application
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkStatic
import no.nav.paw.model.Identitetsnummer
import no.nav.paw.pdl.client.PdlClient
import no.nav.paw.test.data.periode.PeriodeFactory
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.naw.paw.ledigestillinger.model.Arbeidsgiver
import no.naw.paw.ledigestillinger.model.FinnStillingerResponse
import no.naw.paw.ledigestillinger.model.Frist
import no.naw.paw.ledigestillinger.model.FristType
import no.naw.paw.ledigestillinger.model.Kategori
import no.naw.paw.ledigestillinger.model.Lokasjon
import no.naw.paw.ledigestillinger.model.PagingResponse
import no.naw.paw.ledigestillinger.model.Sektor
import no.naw.paw.ledigestillinger.model.Stilling
import no.naw.paw.ledigestillinger.model.StillingStatus
import no.naw.paw.ledigestillinger.model.Stillingsprosent
import no.naw.paw.minestillinger.api.MineStillingerResponse
import no.naw.paw.minestillinger.brukerprofil.beskyttetadresse.harBeskyttetAdresse
import no.naw.paw.minestillinger.db.initDatabase
import no.naw.paw.minestillinger.db.ops.databaseConfigFrom
import no.naw.paw.minestillinger.db.ops.opprettOgOppdaterBruker
import no.naw.paw.minestillinger.db.ops.postgreSQLContainer
import no.naw.paw.minestillinger.domain.BrukerId
import no.naw.paw.minestillinger.domain.Fylke
import no.naw.paw.minestillinger.domain.Kommune
import no.naw.paw.minestillinger.domain.LagretStillingsoek
import no.naw.paw.minestillinger.domain.StedSoek
import no.naw.paw.minestillinger.domain.StillingssoekType
import no.naw.paw.minestillinger.route.MINE_LEDIGE_STILLINGER_PATH
import no.naw.paw.minestillinger.route.ledigeStillingerRoute
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS
import java.util.*

class LedigeStillingerRouteTest : FreeSpec({
    val oauthServer = MockOAuth2Server()
    val postgres = postgreSQLContainer()
    val databaseConfig = databaseConfigFrom(postgres)
    val dataSource = autoClose(initDatabase(databaseConfig))
    beforeSpec {
        oauthServer.start()
        Database.connect(dataSource)
    }
    afterSpec { oauthServer.shutdown() }
    val pdlClient: PdlClient = mockk()

    "Happy path" {
        testApplication {
            application {
                configureKtorServer(
                    prometheusRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
                    meterBinders = emptyList(),
                    authProviders = listOf(oauthServer.tokenXAuthProvider)
                )
            }
            val periode = PeriodeFactory.create().build()
            val ledigeStillingerClient = mockk<FinnStillingerClient>()
            transaction {
                opprettOgOppdaterBruker(periode)
            }
            mockkStatic(PdlClient::harBeskyttetAdresse)
            coEvery { pdlClient.harBeskyttetAdresse(Identitetsnummer(periode.identitetsnummer)) } returns false
            coEvery { ledigeStillingerClient.finnLedigeStillinger(any(), any()) } returns finnStillingerResponse
            routing {
                ledigeStillingerRoute(
                    ledigeStillingerClient = ledigeStillingerClient,
                    hentBrukerId = { BrukerId(10) },
                    hentLagretSøk = { lagreSøk },
                )
            }

            val testIdent = Identitetsnummer(periode.identitetsnummer)

            val response = testClient().get(MINE_LEDIGE_STILLINGER_PATH) {
                bearerAuth(oauthServer.sluttbrukerToken(id = testIdent))
                contentType(Application.Json)
            }
            response.validateAgainstOpenApiSpec()
            response.body<MineStillingerResponse>() should { response ->
                val kilde = finnStillingerResponse.stillinger.associateBy { it.uuid }
                response.resultat.forEach { stilling ->
                    val forventet = withClue("Stilling med UUID ${stilling.arbeidsplassenNoId} skulle ikke vært her, den er ikke i kilden") { kilde[stilling.arbeidsplassenNoId].shouldNotBeNull() }
                    stilling.tittel shouldBe forventet.tittel
                    stilling.selskap shouldBe forventet.arbeidsgivernavn
                    stilling.sektor.name shouldBeEqualIgnoringCase forventet.sektor.name
                    stilling.soeknadsfrist.raw shouldBe forventet.soeknadsfrist.verdi
                    stilling.soeknadsfrist.type.name shouldBeEqualIgnoringCase forventet.soeknadsfrist.type.name
                    stilling.soeknadsfrist.dato shouldBe forventet.soeknadsfrist.dato
                }
            }
            response.status shouldBe HttpStatusCode.OK
        }
    }
})

val finnStillingerResponse = FinnStillingerResponse(
    stillinger = listOf(
        Stilling(
            uuid = UUID.randomUUID(),
            tittel = "Rust utvikler",
            status = StillingStatus.AKTIV,
            stillingsprosent = Stillingsprosent.HELTID,
            sektor = Sektor.OFFENTLIG,
            soeknadsfrist = Frist(type = FristType.DATO, verdi = "${LocalDate.now()}", dato = LocalDate.now()),
            oppstartsfrist = Frist(type = FristType.SNAREST, verdi = "Snarest", dato = null),
            kategorier = listOf(
                Kategori(kode = "0341.11", normalisertKode = "0341", navn = "Rust utvikler"),
                Kategori(kode = "0341.12", normalisertKode = "0341", navn = "Rusten utvikler")
            ),
            lokasjoner = listOf(
                Lokasjon(
                    land = "Norge",
                    postkode = "3050",
                    poststed = "Mjøndalen",
                    kommune = "Drammen",
                    kommunenummer = "3050",
                    fylke = "Drammen",
                    fylkesnummer = "30"
                ),
                Lokasjon(
                    land = "Norge",
                    postkode = "3050",
                    poststed = "Bergen",
                    kommune = "Bergen",
                    kommunenummer = "3050",
                    fylke = "Drammen",
                    fylkesnummer = "30"
                ),
            ),
            publisert = Instant.now(),
            adnr = "Jaha ja",
            arbeidsgivernavn = "Rusty Bucket Bay",
            arbeidsgiver = Arbeidsgiver(
                orgForm = "AS",
                navn = "Rusty Bucket Bay Inc",
                offentligNavn = "Rusty Bucket Bay",
                orgNr = "123123123",
                parentOrgNr = "12313"

            ),
            stillingstittel = "Rusten Utvikler",
            ansettelsesform = "Fast",
            stillingsantall = 10,
            utloeper = Instant.now().plus(10, DAYS),
        )
    ),
    paging = PagingResponse(
        page = 1,
        pageSize = 10,
    )
)

val lagreSøk = listOf(
    LagretStillingsoek(
        id = 1,
        brukerId = 1,
        opprettet = Instant.now(),
        sistKjoet = Instant.now(),
        soek = StedSoek(
            soekType = StillingssoekType.STED_SOEK_V1,
            soekeord = listOf("Utvikler"),
            fylker = listOf(
                Fylke(
                    navn = "Oslo",
                    fylkesnummer = "03",
                    kommuner = listOf(
                        Kommune(kommunenummer = "0301", navn = "Oslo")
                    )
                )
            ),
            styrk08 = listOf("3471"),
        )
    )
)
