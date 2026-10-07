package no.nav.paw.oppslagapi.routes.kontrakt

import tools.jackson.module.kotlin.jacksonMapperBuilder
import tools.jackson.module.kotlin.readValue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import no.nav.paw.error.model.Data
import no.nav.paw.error.model.ProblemDetails
import no.nav.paw.oppslagapi.mapping.v3.asV3
import no.nav.paw.oppslagapi.mapping.v3.finnSistePeriode
import no.nav.paw.oppslagapi.model.v3.AggregertPeriode
import no.nav.paw.oppslagapi.model.v3.Annet
import no.nav.paw.oppslagapi.model.v3.Helse
import no.nav.paw.oppslagapi.model.v3.Hendelse
import no.nav.paw.oppslagapi.model.v3.JaNeiVetIkke
import no.nav.paw.oppslagapi.model.v3.OpplysningerOmArbeidssoeker
import no.nav.paw.oppslagapi.model.v3.PaaVegneAvStopp
import no.nav.paw.oppslagapi.model.v3.IdentitetsnummerQueryRequest
import no.nav.paw.oppslagapi.model.v3.SortOrder
import no.nav.paw.oppslagapi.model.v3.Tidslinje
import no.nav.paw.oppslagapi.routes.v3.sortedByStart
import no.nav.paw.oppslagapi.routes.v3.sortedByTidspunk
import no.nav.paw.oppslagapi.test.TestContext
import no.nav.paw.oppslagapi.test.TestData
import no.nav.paw.oppslagapi.test.ansattToken
import no.nav.paw.oppslagapi.test.configureMock
import no.nav.paw.oppslagapi.test.hentViaPost
import no.nav.paw.oppslagapi.utils.configureJacksonForV3

/**
 * JSON-kontrakttester for /api/v3.
 *
 * Fasitfilene i `src/test/resources/json-kontrakt/api/v3/` må aldri endres; de låser formatet vi sender
 * til konsumenter. Responsen kommer fra de ekte rutene med ekte ContentNegotiation (configureJacksonForV3).
 * Hver test sjekker at (1) responsen er lik fasitfilen, og (2) at fasitfilen leses inn til forventet objekt
 * med samme produksjonsmapper.
 */
class V3JsonKontraktTest : FreeSpec({
    val v3Mapper = jacksonMapperBuilder().configureJacksonForV3().build()
    val request = IdentitetsnummerQueryRequest(identitetsnummer = KontraktTestData.identitetsnummer)

    with(TestContext()) {
        beforeSpec {
            tilgangsTjenesteForAnsatteMock.configureMock()
            mockOAuthServer.start()
        }
        afterSpec { mockOAuthServer.shutdown() }

        fun forventetSnapshot(perioder: List<java.util.UUID>): AggregertPeriode =
            (Data(KontraktTestData.tidslinjer(perioder).map { it.asV3() }).finnSistePeriode() as Data).data

        "POST /api/v3/snapshot" - {
            "fullt utfylt, avsluttet periode" {
                val perioder = listOf(KontraktTestData.periode1Id)
                kontraktTestApplication(perioder) { client ->
                    val response = client.hentViaPost(
                        "/api/v3/snapshot", mockOAuthServer.ansattToken(TestData.ansatt1), request
                    )
                    response.status shouldBe HttpStatusCode.OK
                    response.bodyAsText().skalVaereLikFasit("v3/snapshot-fullt-utfylt.json")
                    v3Mapper.readValue<AggregertPeriode>(lesFasit("v3/snapshot-fullt-utfylt.json")) shouldBe
                            forventetSnapshot(perioder).somLestInnFraV3Json()
                }
            }
            "aktiv periode med valgfrie felter null" {
                val perioder = listOf(KontraktTestData.periode3Id)
                kontraktTestApplication(perioder) { client ->
                    val response = client.hentViaPost(
                        "/api/v3/snapshot", mockOAuthServer.ansattToken(TestData.ansatt1), request
                    )
                    response.status shouldBe HttpStatusCode.OK
                    response.bodyAsText().skalVaereLikFasit("v3/snapshot-minimal.json")
                    v3Mapper.readValue<AggregertPeriode>(lesFasit("v3/snapshot-minimal.json")) shouldBe
                            forventetSnapshot(perioder).somLestInnFraV3Json()
                }
            }
            "aktiv periode med null i nøstede felter" {
                val perioder = listOf(KontraktTestData.periode2Id)
                kontraktTestApplication(perioder) { client ->
                    val response = client.hentViaPost(
                        "/api/v3/snapshot", mockOAuthServer.ansattToken(TestData.ansatt1), request
                    )
                    response.status shouldBe HttpStatusCode.OK
                    response.bodyAsText().skalVaereLikFasit("v3/snapshot-nullfelter.json")
                    v3Mapper.readValue<AggregertPeriode>(lesFasit("v3/snapshot-nullfelter.json")) shouldBe
                            forventetSnapshot(perioder).somLestInnFraV3Json()
                }
            }
            "404 ProblemDetails når personen ikke har perioder" {
                kontraktTestApplication(perioder = emptyList()) { client ->
                    val response = client.hentViaPost(
                        "/api/v3/snapshot", mockOAuthServer.ansattToken(TestData.ansatt1), request
                    )
                    response.status shouldBe HttpStatusCode.NotFound
                    val normalisert = normaliserProblemDetails(response.bodyAsText())
                    normalisert.skalVaereLikFasit("v3/problemdetails-404-periode-ikke-funnet.json")
                    v3Mapper.readValue<ProblemDetails>(lesFasit("v3/problemdetails-404-periode-ikke-funnet.json")) shouldBe
                            v3Mapper.readValue<ProblemDetails>(normalisert)
                }
            }
            "403 ProblemDetails når ansatt mangler tilgang" {
                kontraktTestApplication { client ->
                    val response = client.hentViaPost(
                        "/api/v3/snapshot", mockOAuthServer.ansattToken(TestData.ansatt3), request
                    )
                    response.status shouldBe HttpStatusCode.Forbidden
                    val normalisert = normaliserProblemDetails(response.bodyAsText())
                    normalisert.skalVaereLikFasit("v3/problemdetails-403-ingen-tilgang.json")
                    v3Mapper.readValue<ProblemDetails>(lesFasit("v3/problemdetails-403-ingen-tilgang.json")) shouldBe
                            v3Mapper.readValue<ProblemDetails>(normalisert)
                }
            }
        }

        "POST /api/v3/perioder" - {
            "alle hendelsestyper, fullt utfylt og med null-felter" {
                kontraktTestApplication { client ->
                    val response = client.hentViaPost(
                        "/api/v3/perioder", mockOAuthServer.ansattToken(TestData.ansatt1), request
                    )
                    response.status shouldBe HttpStatusCode.OK
                    response.bodyAsText().skalVaereLikFasit("v3/perioder-alle-hendelsestyper.json")
                    val forventet = KontraktTestData.tidslinjer()
                        .map { it.asV3() }
                        .map { it.copy(hendelser = it.hendelser.sortedByTidspunk(SortOrder.DESC)) }
                        .sortedByStart(SortOrder.DESC)
                    v3Mapper.readValue<List<Tidslinje>>(lesFasit("v3/perioder-alle-hendelsestyper.json")) shouldBe
                            forventet.map { it.somLestInnFraV3Json() }
                }
            }
            "400 ProblemDetails ved ugyldig types-parameter" {
                kontraktTestApplication { client ->
                    val response = client.hentViaPost(
                        "/api/v3/perioder?types=UKJENT", mockOAuthServer.ansattToken(TestData.ansatt1), request
                    )
                    response.status shouldBe HttpStatusCode.BadRequest
                    val normalisert = normaliserProblemDetails(response.bodyAsText())
                    normalisert.skalVaereLikFasit("v3/problemdetails-400-ugyldig-forespoersel.json")
                    v3Mapper.readValue<ProblemDetails>(lesFasit("v3/problemdetails-400-ugyldig-forespoersel.json")) shouldBe
                            v3Mapper.readValue<ProblemDetails>(normalisert)
                }
            }
        }
    }
})

/*
 * v3 bruker JsonInclude.NON_NULL, så null-felter utelates i JSON. Ved innlesing fyller Kotlin-modulen inn
 * standardverdien fra konstruktøren i stedet for null. For tre nullbare felter er standardverdien ikke null,
 * så null overlever ikke en runde gjennom v3-JSON:
 *  - Helse.helsetilstandHindrerArbeid: null -> UKJENT_VERDI
 *  - Annet.andreForholdHindrerArbeid: null -> UKJENT_VERDI
 *  - PaaVegneAvStopp.fristBrutt: null -> false
 * Funksjonene under gjør den samme omformingen på forventet objekt, slik at testen dokumenterer avviket
 * i stedet for å skjule det.
 */
private fun AggregertPeriode.somLestInnFraV3Json(): AggregertPeriode =
    copy(opplysning = opplysning?.somLestInnFraV3Json())

private fun Tidslinje.somLestInnFraV3Json(): Tidslinje =
    copy(hendelser = hendelser.map { it.somLestInnFraV3Json() })

private fun Hendelse.somLestInnFraV3Json(): Hendelse = when (this) {
    is OpplysningerOmArbeidssoeker -> somLestInnFraV3Json()
    is PaaVegneAvStopp -> copy(fristBrutt = fristBrutt ?: false)
    else -> this
}

private fun OpplysningerOmArbeidssoeker.somLestInnFraV3Json(): OpplysningerOmArbeidssoeker = copy(
    helse = helse?.let { Helse(helsetilstandHindrerArbeid = it.helsetilstandHindrerArbeid ?: JaNeiVetIkke.UKJENT_VERDI) },
    annet = annet?.let { Annet(andreForholdHindrerArbeid = it.andreForholdHindrerArbeid ?: JaNeiVetIkke.UKJENT_VERDI) }
)
