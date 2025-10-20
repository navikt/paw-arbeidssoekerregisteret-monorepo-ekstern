package no.naw.paw.minestillinger

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.put
import io.ktor.http.ContentType.Application
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.mockk
import no.nav.paw.model.Identitetsnummer
import no.nav.paw.pdl.client.PdlClient
import no.nav.paw.test.data.periode.PeriodeFactory
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.naw.paw.minestillinger.api.vo.ApiTjenesteStatus
import no.naw.paw.minestillinger.db.initDatabase
import no.naw.paw.minestillinger.db.ops.databaseConfigFrom
import no.naw.paw.minestillinger.db.ops.opprettOgOppdaterBruker
import no.naw.paw.minestillinger.db.ops.postgreSQLContainer
import no.naw.paw.minestillinger.db.ops.setTjenestatus
import no.naw.paw.minestillinger.domain.TjenesteStatus
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

data class TjenestestatusTestCase(
    val identitetsnummer: Identitetsnummer,
    val gjeldendeTjenestestatus: TjenesteStatus,
    val nyTjenesteStatus: ApiTjenesteStatus,
    val forventetHttpStatusKode: HttpStatusCode,
) {
    override fun toString() =
        "Endring av tjenestestatus fra $gjeldendeTjenestestatus til $nyTjenesteStatus " +
                "for bruker $identitetsnummer forventer HTTP status $forventetHttpStatusKode"
}

val testcases = listOf(
    TjenestestatusTestCase(
        identitetsnummer = Identitetsnummer("12345678901"),
        gjeldendeTjenestestatus = TjenesteStatus.AKTIV,
        nyTjenesteStatus = ApiTjenesteStatus.INAKTIV,
        forventetHttpStatusKode = HttpStatusCode.NoContent,
    ),
    TjenestestatusTestCase(
        identitetsnummer = Identitetsnummer("12345678901"),
        gjeldendeTjenestestatus = TjenesteStatus.INAKTIV,
        nyTjenesteStatus = ApiTjenesteStatus.AKTIV,
        forventetHttpStatusKode = HttpStatusCode.NoContent,
    ),
    TjenestestatusTestCase(
        identitetsnummer = Identitetsnummer("12345678901"),
        gjeldendeTjenestestatus = TjenesteStatus.AKTIV,
        nyTjenesteStatus = ApiTjenesteStatus.OPT_OUT,
        forventetHttpStatusKode = HttpStatusCode.NoContent,
    ),
    TjenestestatusTestCase(
        identitetsnummer = Identitetsnummer("12345678902"),
        gjeldendeTjenestestatus = TjenesteStatus.KAN_IKKE_LEVERES,
        nyTjenesteStatus = ApiTjenesteStatus.AKTIV,
        forventetHttpStatusKode = HttpStatusCode.Forbidden,
    ),
    TjenestestatusTestCase(
        identitetsnummer = Identitetsnummer("12345678902"),
        gjeldendeTjenestestatus = TjenesteStatus.KAN_IKKE_LEVERES,
        nyTjenesteStatus = ApiTjenesteStatus.INAKTIV,
        forventetHttpStatusKode = HttpStatusCode.Forbidden,
    ),
)

class TjenestestatusE2ETest : FreeSpec({
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

    testcases.forEach { testcase ->
        transaction {
            opprettOgOppdaterBruker(PeriodeFactory.create().build(identitetsnummer = testcase.identitetsnummer.verdi))
            setTjenestatus(
                identitetsnummer = testcase.identitetsnummer,
                nyTjenestestatus = testcase.gjeldendeTjenestestatus,
            )
        }

        "$testcase" {
            testApplication {
                application {
                    configureKtorServer(
                        prometheusRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
                        meterBinders = emptyList(),
                        authProviders = listOf(oauthServer.tokenXAuthProvider)
                    )
                }
                routing { brukerprofilRoute(brukerprofilTjeneste) }

                val response = testClient().put("$BRUKERPROFIL_PATH/tjenestestatus/${testcase.nyTjenesteStatus.name}") {
                    bearerAuth(oauthServer.sluttbrukerToken(id = testcase.identitetsnummer))
                    contentType(Application.Json)
                }
                response.validateAgainstOpenApiSpec()
                response.status shouldBe testcase.forventetHttpStatusKode
            }
        }
    }
})
