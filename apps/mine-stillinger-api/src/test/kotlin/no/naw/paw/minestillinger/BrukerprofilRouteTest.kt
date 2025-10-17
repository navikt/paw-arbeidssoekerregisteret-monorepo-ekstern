package no.naw.paw.minestillinger

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.put
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
import no.naw.paw.minestillinger.api.vo.ApiBrukerprofil
import no.naw.paw.minestillinger.db.initDatabase
import no.naw.paw.minestillinger.db.ops.databaseConfigFrom
import no.naw.paw.minestillinger.db.ops.opprettOgOppdaterBruker
import no.naw.paw.minestillinger.db.ops.postgreSQLContainer
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
    val brukerprofilTjeneste = BrukerprofilTjeneste(pdlClient)

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
            routing { brukerprofilRoute(brukerprofilTjeneste) }

            val testIdent = Identitetsnummer(periode.identitetsnummer)

            val response = testClient().get(BRUKERPROFIL_PATH) {
                bearerAuth(oauthServer.sluttbrukerToken(id = testIdent))
                contentType(Application.Json)
            }
            response.validateAgainstOpenApiSpec()
            response.status shouldBe HttpStatusCode.OK
            response.body<ApiBrukerprofil>() should { profil ->
                profil.identitetsnummer shouldBe testIdent.verdi
                profil.erIkkeInteressert shouldBe false
                profil.erTjenestenLedigeStillingerAktiv shouldBe false
            }
            val responseSetAktiv = testClient().put("$ER_TJENESTEN_AKTIV_PATH/true") {
                bearerAuth(oauthServer.sluttbrukerToken(id = testIdent))
                contentType(Application.Json)
            }
            responseSetAktiv.validateAgainstOpenApiSpec()
            responseSetAktiv.status shouldBe HttpStatusCode.NoContent
            val responseSetIkkeIntr = testClient().put("$ER_IKKE_INTERESSERT_PATH/true") {
                bearerAuth(oauthServer.sluttbrukerToken(id = testIdent))
                contentType(Application.Json)
            }
            responseSetIkkeIntr.validateAgainstOpenApiSpec()
            responseSetIkkeIntr.status shouldBe HttpStatusCode.NoContent
            val brukerProfilEtterOppdatering = testClient().get(BRUKERPROFIL_PATH) {
                bearerAuth(oauthServer.sluttbrukerToken(id = testIdent))
                contentType(Application.Json)
            }
            brukerProfilEtterOppdatering.validateAgainstOpenApiSpec()
            brukerProfilEtterOppdatering.status shouldBe HttpStatusCode.OK
            brukerProfilEtterOppdatering.body<ApiBrukerprofil>() should { profil ->
                profil.identitetsnummer shouldBe testIdent.verdi
                profil.erIkkeInteressert shouldBe true
                profil.erTjenestenLedigeStillingerAktiv shouldBe true
            }
        }
    }

})
