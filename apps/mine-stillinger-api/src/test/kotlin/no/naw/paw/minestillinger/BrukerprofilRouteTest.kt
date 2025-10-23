package no.naw.paw.minestillinger

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
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
import no.naw.paw.minestillinger.domain.ReiseveiSoek
import no.naw.paw.minestillinger.domain.StillingssoekType
import no.naw.paw.minestillinger.route.BRUKERPROFIL_PATH
import no.naw.paw.minestillinger.route.brukerprofilRoute
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class BrukerprofilRouteTest : FreeSpec({
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
        slettAlleSøk = ::slettAlleSoekForBruker
    )

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
            transaction {
                opprettOgOppdaterBruker(periode)
            }
            mockkStatic(PdlClient::harBeskyttetAdresse)
            coEvery { pdlClient.harBeskyttetAdresse(Identitetsnummer(periode.identitetsnummer)) } returns false
            routing { brukerprofilRoute(
                brukerprofilTjeneste = brukerprofilTjeneste,
                søkeAdminOps = ExposedSøkAdminOps
            ) }

            val testIdent = Identitetsnummer(periode.identitetsnummer)

            val response = testClient().get(BRUKERPROFIL_PATH) {
                bearerAuth(oauthServer.sluttbrukerToken(id = testIdent))
                contentType(Application.Json)
            }
            response.validateAgainstOpenApiSpec()
            response.status shouldBe HttpStatusCode.OK
            response.body<ApiBrukerprofil>() should { profil ->
                profil.identitetsnummer shouldBe testIdent.verdi
                profil.tjenestestatus shouldBe ApiTjenesteStatus.INAKTIV
            }
            val brukerProfilEtterOppdatering = testClient().get(BRUKERPROFIL_PATH) {
                bearerAuth(oauthServer.sluttbrukerToken(id = testIdent))
                contentType(Application.Json)
            }
            brukerProfilEtterOppdatering.validateAgainstOpenApiSpec()
            brukerProfilEtterOppdatering.status shouldBe HttpStatusCode.OK
            brukerProfilEtterOppdatering.body<ApiBrukerprofil>() should { profil ->
                profil.identitetsnummer shouldBe testIdent.verdi

            }
        }
    }

    "Skal kunne oppdatere søk for bruker" {
        val periode = PeriodeFactory.create().build()
        transaction {
            opprettOgOppdaterBruker(periode)
        }

        testApplication {
            application {
                configureKtorServer(
                    prometheusRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
                    meterBinders = emptyList(),
                    authProviders = listOf(oauthServer.tokenXAuthProvider)
                )
            }
            routing { brukerprofilRoute(
                brukerprofilTjeneste = brukerprofilTjeneste,
                søkeAdminOps = ExposedSøkAdminOps
            ) }
            val response = testClient().put("$BRUKERPROFIL_PATH/stillingssoek") {
                bearerAuth(oauthServer.sluttbrukerToken(id = Identitetsnummer(periode.identitetsnummer)))
                contentType(Application.Json)
                setBody(
                    listOf(
                        ApiStedSoek(
                            soekType = ApiStillingssoekType.STED_SOEK_V1,
                            fylker = listOf(
                                ApiFylke(
                                    navn = "Oslo",
                                    fylkesnummer = "03",
                                    kommuner = listOf(
                                        ApiKommune(kommunenummer = "0301", navn = "Oslo")
                                    )
                                )
                            ),
                            soekeord = listOf("Tryllekunstner"),
                            styrk08Kode = listOf("3471")
                        ),
                        ReiseveiSoek(
                            soekType = StillingssoekType.REISEVEI_SOEK_V1,
                            soekeord = listOf("Utvikler"),
                            maksAvstandKm = 40,
                            postnummer = "0555",
                        )
                    )
                )
            }

            response.validateAgainstOpenApiSpec()
            response.status shouldBe HttpStatusCode.NoContent
        }
    }
})
