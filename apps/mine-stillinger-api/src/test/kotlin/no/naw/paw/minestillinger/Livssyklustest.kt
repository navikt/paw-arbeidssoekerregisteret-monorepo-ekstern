package no.naw.paw.minestillinger

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
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
import no.naw.paw.minestillinger.api.ApiStedSoek
import no.naw.paw.minestillinger.api.ApiStillingssoek
import no.naw.paw.minestillinger.api.vo.ApiBrukerprofil
import no.naw.paw.minestillinger.api.vo.ApiFylke
import no.naw.paw.minestillinger.api.vo.ApiKommune
import no.naw.paw.minestillinger.api.vo.ApiStillingssoekType
import no.naw.paw.minestillinger.api.vo.ApiTjenesteStatus
import no.naw.paw.minestillinger.brukerprofil.BrukerprofilTjeneste
import no.naw.paw.minestillinger.brukerprofil.beskyttetadresse.harBeskyttetAdresse
import no.naw.paw.minestillinger.db.initDatabase
import no.naw.paw.minestillinger.db.ops.ExposedSøkAdminOps
import no.naw.paw.minestillinger.db.ops.databaseConfigFrom
import no.naw.paw.minestillinger.db.ops.hentBrukerProfilUtenFlagg
import no.naw.paw.minestillinger.db.ops.hentProfileringOrNull
import no.naw.paw.minestillinger.db.ops.lesFlaggFraDB
import no.naw.paw.minestillinger.db.ops.opprettOgOppdaterBruker
import no.naw.paw.minestillinger.db.ops.postgreSQLContainer
import no.naw.paw.minestillinger.db.ops.skrivFlaggTilDB
import no.naw.paw.minestillinger.db.ops.slettAlleSoekForBruker
import no.naw.paw.minestillinger.domain.TjenesteStatus.KAN_IKKE_LEVERES
import no.naw.paw.minestillinger.route.BRUKERPROFIL_PATH
import no.naw.paw.minestillinger.route.brukerprofilRoute
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicReference

class Livssyklustest : FreeSpec({
    val currentTime = AtomicReference(Instant.EPOCH)
    val clock = object : Clock {
        override fun now(): Instant = currentTime.get()
    }

    fun forwardTimeByHours(hours: Long) {
        currentTime.updateAndGet { it.plus(hours, ChronoUnit.HOURS) }
    }

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
    val brukerprofilTjeneste = BrukerprofilTjeneste(
        pdlClient = pdlClient,
        hentBrukerprofilUtenFlagg = ::hentBrukerProfilUtenFlagg,
        skrivFlagg = ::skrivFlaggTilDB,
        hentFlagg = ::lesFlaggFraDB,
        hentProfilering = ::hentProfileringOrNull,
        slettAlleSøk = ::slettAlleSoekForBruker,
        abTestingRegex = Regex(""),
        clock = clock
    )
    "Full test av av livessyklus for brukerprofil" - {
        testApplication {
            application {
                configureKtorServer(
                    prometheusRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
                    meterBinders = emptyList(),
                    authProviders = listOf(oauthServer.tokenXAuthProvider)
                )
            }
            val testClient = testClient()
            val periode = PeriodeFactory.create().build()
            currentTime.set(periode.startet.tidspunkt)
            val testIdent = Identitetsnummer(periode.identitetsnummer)
            mockkStatic(PdlClient::harBeskyttetAdresse)
            coEvery { pdlClient.harBeskyttetAdresse(Identitetsnummer(periode.identitetsnummer)) } returns false

            routing {
                brukerprofilRoute(
                    brukerprofilTjeneste = brukerprofilTjeneste,
                    søkeAdminOps = ExposedSøkAdminOps,
                    clock = clock
                )
            }

            "Når ingen brukerprofil er opprettet får vi 404" {
                val response = testClient.get(BRUKERPROFIL_PATH) {
                    bearerAuth(oauthServer.sluttbrukerToken(id = testIdent))
                    contentType(Application.Json)
                }
                response.validateAgainstOpenApiSpec()
                response.status shouldBe HttpStatusCode.NotFound
            }

            transaction {
                opprettOgOppdaterBruker(periode)
            }

            "Når brukerprofilen er opprettet får vi 200 OK med inaktiv tjenestestatus" {
                val response = testClient.get(BRUKERPROFIL_PATH) {
                    bearerAuth(oauthServer.sluttbrukerToken(id = testIdent))
                    contentType(Application.Json)
                }
                response.validateAgainstOpenApiSpec()
                response.status shouldBe HttpStatusCode.OK
                response.body<ApiBrukerprofil>() should { profil ->
                    profil.identitetsnummer shouldBe testIdent.verdi
                    profil.tjenestestatus shouldBe ApiTjenesteStatus.INAKTIV
                    profil.stillingssoek.shouldBeEmpty()
                }
            }

            "Når brukerprofilen har gradert adresse får vi 200 OK med ${KAN_IKKE_LEVERES} tjenestestatus" {
                coEvery { pdlClient.harBeskyttetAdresse(testIdent) } returns true
                val response = testClient.get(BRUKERPROFIL_PATH) {
                    bearerAuth(oauthServer.sluttbrukerToken(id = testIdent))
                    contentType(Application.Json)
                }
                response.validateAgainstOpenApiSpec()
                response.status shouldBe HttpStatusCode.OK
                response.body<ApiBrukerprofil>() should { profil ->
                    profil.identitetsnummer shouldBe testIdent.verdi
                    profil.tjenestestatus shouldBe ApiTjenesteStatus.KAN_IKKE_LEVERES
                    profil.stillingssoek.shouldBeEmpty()
                }
            }

            "Når vi prøver å starte tjenesten får vi 403 forbidden" {
                val response = testClient.put("${BRUKERPROFIL_PATH}/tjenestestatus/AKTIV") {
                    bearerAuth(oauthServer.sluttbrukerToken(id = testIdent))
                    contentType(Application.Json)
                }
                response.validateAgainstOpenApiSpec()
                response.status shouldBe HttpStatusCode.Forbidden
            }

            "Nå vi prøver å lagre et søk får vi 403 forbidden" {
                val response = testClient.put("$BRUKERPROFIL_PATH/stillingssoek") {
                    bearerAuth(oauthServer.sluttbrukerToken(id = testIdent))
                    contentType(Application.Json)
                    setBody(
                        listOf(
                            ApiStedSoek(
                                soekType = ApiStillingssoekType.STED_SOEK_V1,
                                fylker = listOf(
                                    ApiFylke(
                                        navn = "Vestland",
                                        fylkesnummer = "46",
                                        kommuner = listOf(
                                            ApiKommune(
                                                navn = "Bergen",
                                                kommunenummer = "4601"
                                            )
                                        )
                                    )
                                ),
                                soekeord = emptyList(),
                                styrk08 = emptyList()
                            )
                        )
                    )
                }
                response.validateAgainstOpenApiSpec()
                response.status shouldBe HttpStatusCode.Forbidden
            }
            "Når gradert adresse fjernes får vi 204 NO Content med INAKTIV tjenestestatus" {
                forwardTimeByHours(36)
                coEvery { pdlClient.harBeskyttetAdresse(testIdent) } returns false
                val response = testClient.get(BRUKERPROFIL_PATH) {
                    bearerAuth(oauthServer.sluttbrukerToken(id = testIdent))
                    contentType(Application.Json)
                }
                response.validateAgainstOpenApiSpec()
                response.status shouldBe HttpStatusCode.NoContent
                response.body<ApiBrukerprofil>() should { profil ->
                    profil.identitetsnummer shouldBe testIdent.verdi
                    profil.tjenestestatus shouldBe ApiTjenesteStatus.INAKTIV
                    profil.stillingssoek.shouldBeEmpty()
                }
            }
            "Vi kan nå lagre et søk" {
                val response = testClient.put("$BRUKERPROFIL_PATH/stillingssoek") {
                    bearerAuth(oauthServer.sluttbrukerToken(id = testIdent))
                    contentType(Application.Json)
                    setBody(
                        listOf(
                            ApiStedSoek(
                                soekType = ApiStillingssoekType.STED_SOEK_V1,
                                fylker = listOf(
                                    ApiFylke(
                                        navn = "Vestland",
                                        fylkesnummer = "46",
                                        kommuner = listOf(
                                            ApiKommune(
                                                navn = "Bergen",
                                                kommunenummer = "4601"
                                            )
                                        )
                                    )
                                ),
                                soekeord = emptyList(),
                                styrk08 = emptyList()
                            )
                        )
                    )
                }
                response.validateAgainstOpenApiSpec()
                response.status shouldBe HttpStatusCode.NoContent
            }
            "Brukerprofilen skal nå inneholde et søk" {
                val response = testClient.get(BRUKERPROFIL_PATH) {
                    bearerAuth(oauthServer.sluttbrukerToken(id = testIdent))
                    contentType(Application.Json)
                }
                response.validateAgainstOpenApiSpec()
                response.status shouldBe HttpStatusCode.OK
                response.body<ApiBrukerprofil>() should { profil ->
                    profil.identitetsnummer shouldBe testIdent.verdi
                    profil.tjenestestatus shouldBe ApiTjenesteStatus.INAKTIV
                    profil.stillingssoek.shouldHaveSize(1)
                    profil.stillingssoek.firstOrNull() should { søk ->
                        søk.shouldNotBeNull()
                        søk.shouldBeInstanceOf<ApiStedSoek>()
                        søk.fylker.firstOrNull() should { fylke ->
                            fylke.shouldNotBeNull()
                            fylke.fylkesnummer shouldBe "46"
                            fylke.navn shouldBe "Vestland"
                            fylke.kommuner.firstOrNull() should { kommune ->
                                kommune.shouldNotBeNull()
                                kommune.kommunenummer shouldBe "4601"
                                kommune.navn shouldBe "Bergen"
                            }
                        }
                    }
                }
            }
        }
    }
})