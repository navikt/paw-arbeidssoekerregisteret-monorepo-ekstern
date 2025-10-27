package no.naw.paw.minestillinger

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.put
import io.ktor.http.ContentType.Application
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.mockk
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import no.nav.paw.model.Identitetsnummer
import no.nav.paw.pdl.client.PdlClient
import no.nav.paw.test.data.periode.PeriodeFactory
import no.nav.paw.test.data.periode.createProfilering
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.naw.paw.minestillinger.api.vo.ApiTjenesteStatus
import no.naw.paw.minestillinger.brukerprofil.BrukerprofilTjeneste
import no.naw.paw.minestillinger.db.initDatabase
import no.naw.paw.minestillinger.db.ops.databaseConfigFrom
import no.naw.paw.minestillinger.db.ops.hentBrukerProfilUtenFlagg
import no.naw.paw.minestillinger.db.ops.hentProfileringOrNull
import no.naw.paw.minestillinger.db.ops.lesFlaggFraDB
import no.naw.paw.minestillinger.db.ops.opprettOgOppdaterBruker
import no.naw.paw.minestillinger.db.ops.postgreSQLContainer
import no.naw.paw.minestillinger.db.ops.skrivFlaggTilDB
import no.naw.paw.minestillinger.db.ops.slettAlleSoekForBruker
import no.naw.paw.minestillinger.brukerprofil.flagg.ErITestGruppenFlagg
import no.naw.paw.minestillinger.brukerprofil.flagg.HarGodeMuligheterFlagg
import no.naw.paw.minestillinger.brukerprofil.flagg.HarGradertAdresseFlagg
import no.naw.paw.minestillinger.brukerprofil.flagg.OptOutFlag
import no.naw.paw.minestillinger.domain.TjenesteStatus
import no.naw.paw.minestillinger.brukerprofil.flagg.TjenestenErAktivFlagg
import no.naw.paw.minestillinger.brukerprofil.flagg.flaggListeOf
import no.naw.paw.minestillinger.db.ops.ExposedSøkAdminOps
import no.naw.paw.minestillinger.db.ops.lagreProfilering
import no.naw.paw.minestillinger.domain.ProfileringResultat
import no.naw.paw.minestillinger.route.BRUKERPROFIL_PATH
import no.naw.paw.minestillinger.route.brukerprofilRoute
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Instant.now

data class TjenestestatusTestCase(
    val identitetsnummer: Identitetsnummer,
    val gjeldendeTjenestestatus: TjenesteStatus,
    val nyTjenesteStatus: ApiTjenesteStatus,
    val forventetHttpStatusKode: HttpStatusCode,
    val profilertTil: ProfileringResultat? = ProfileringResultat.ANTATT_GODE_MULIGHETER
) {
    override fun toString() =
        "Endring av tjenestestatus fra $gjeldendeTjenestestatus til $nyTjenesteStatus " +
                "for bruker ${identitetsnummer.verdi} forventer HTTP status $forventetHttpStatusKode"
}

val testcases = listOf(
    TjenestestatusTestCase(
        identitetsnummer = Identitetsnummer("12345678901"),
        gjeldendeTjenestestatus = TjenesteStatus.AKTIV,
        nyTjenesteStatus = ApiTjenesteStatus.INAKTIV,
        forventetHttpStatusKode = HttpStatusCode.NoContent,
    ),
    TjenestestatusTestCase(
        identitetsnummer = Identitetsnummer("12345678902"),
        gjeldendeTjenestestatus = TjenesteStatus.INAKTIV,
        nyTjenesteStatus = ApiTjenesteStatus.AKTIV,
        forventetHttpStatusKode = HttpStatusCode.NoContent,
    ),
    TjenestestatusTestCase(
        identitetsnummer = Identitetsnummer("12345678903"),
        gjeldendeTjenestestatus = TjenesteStatus.AKTIV,
        nyTjenesteStatus = ApiTjenesteStatus.OPT_OUT,
        forventetHttpStatusKode = HttpStatusCode.NoContent,
    ),
    TjenestestatusTestCase(
        identitetsnummer = Identitetsnummer("12345678904"),
        gjeldendeTjenestestatus = TjenesteStatus.KAN_IKKE_LEVERES,
        nyTjenesteStatus = ApiTjenesteStatus.AKTIV,
        forventetHttpStatusKode = HttpStatusCode.Forbidden,
    ),
    TjenestestatusTestCase(
        identitetsnummer = Identitetsnummer("12345678905"),
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
    val brukerprofilTjeneste = BrukerprofilTjeneste(
        pdlClient = pdlClient,
        hentBrukerprofilUtenFlagg = ::hentBrukerProfilUtenFlagg,
        skrivFlagg = ::skrivFlaggTilDB,
        hentFlagg = ::lesFlaggFraDB,
        hentProfilering = ::hentProfileringOrNull,
        slettAlleSøk = ::slettAlleSoekForBruker,
        abTestingRegex = Regex("""\d([0248])\d{9}"""),
    )

    testcases.forEach { testcase ->
        transaction {
            opprettOgOppdaterBruker(PeriodeFactory.create().build(identitetsnummer = testcase.identitetsnummer.verdi))
            val profil = brukerprofilTjeneste.hentBrukerprofilUtenFlagg(testcase.identitetsnummer)!!
            val brukerId = profil.id
            if (testcase.profilertTil == ProfileringResultat.ANTATT_GODE_MULIGHETER) {
                lagreProfilering(
                    createProfilering(
                        periodeId = profil.arbeidssoekerperiodeId.verdi,
                        profilertTil = ProfilertTil.ANTATT_GODE_MULIGHETER
                    )
                )
            }
            when (testcase.gjeldendeTjenestestatus) {
                TjenesteStatus.AKTIV -> skrivFlaggTilDB(
                    brukerId, flaggListeOf(
                        TjenestenErAktivFlagg(true, now()),
                        HarGodeMuligheterFlagg(true, now()),
                        HarGradertAdresseFlagg(false, now())
                    )
                )

                TjenesteStatus.INAKTIV -> skrivFlaggTilDB(
                    brukerId, flaggListeOf(
                        TjenestenErAktivFlagg(false, now()),
                        HarGradertAdresseFlagg(false, now()),
                        ErITestGruppenFlagg(true, now())
                    )
                )

                TjenesteStatus.OPT_OUT -> skrivFlaggTilDB(
                    brukerId, flaggListeOf(
                        TjenestenErAktivFlagg(false, now()),
                        HarGodeMuligheterFlagg(true, now()),
                        OptOutFlag(true, now()),
                        HarGradertAdresseFlagg(false, now())
                    )
                )

                TjenesteStatus.KAN_IKKE_LEVERES -> skrivFlaggTilDB(
                    brukerId, flaggListeOf(
                        TjenestenErAktivFlagg(false, now()),
                        HarGodeMuligheterFlagg(true, now()),
                        HarGradertAdresseFlagg(true, now())
                    )
                )
            }
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
                routing {
                    brukerprofilRoute(
                        brukerprofilTjeneste = brukerprofilTjeneste,
                        søkeAdminOps = ExposedSøkAdminOps
                    )
                }

                val response =
                    testClient().put("${BRUKERPROFIL_PATH}/tjenestestatus/${testcase.nyTjenesteStatus.name}") {
                        bearerAuth(oauthServer.sluttbrukerToken(id = testcase.identitetsnummer))
                        contentType(Application.Json)
                    }
                response.validateAgainstOpenApiSpec()
                withClue(response.body<String>()) {
                    response.status shouldBe testcase.forventetHttpStatusKode
                }
            }
        }
    }
})
