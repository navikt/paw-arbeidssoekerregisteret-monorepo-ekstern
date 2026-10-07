package no.nav.paw.oppslagapi.routes.kontrakt

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import no.nav.paw.error.model.ProblemDetails
import no.nav.paw.oppslagapi.model.v2.BekreftelserResponse
import no.nav.paw.oppslagapi.model.v2.TidslinjeResponse
import no.nav.paw.oppslagapi.model.v2.V2Request
import no.nav.paw.oppslagapi.test.TestContext
import no.nav.paw.oppslagapi.test.TestData
import no.nav.paw.oppslagapi.test.ansattToken
import no.nav.paw.oppslagapi.test.configureMock
import no.nav.paw.oppslagapi.test.hentViaPost
import no.nav.paw.oppslagapi.utils.configureJacksonForV1
import java.time.Instant

/**
 * JSON-kontrakttester for /api/v2.
 *
 * Fasitfilene i `src/test/resources/json-kontrakt/api/v2/` må aldri endres; de låser formatet vi sender
 * til konsumenter. /api/v2 bruker configureJacksonForV1 (null-verdier skrives ut). Responsen kommer fra de
 * ekte rutene. Hver test sjekker at (1) responsen er lik fasitfilen, og (2) at fasitfilen leses inn til
 * forventet objekt med samme produksjonsmapper.
 */
class V2JsonKontraktTest : FreeSpec({
    val v2Mapper = jacksonObjectMapper().configureJacksonForV1()
    val request = V2Request(identitetsnummer = KontraktTestData.identitetsnummer.value, perioder = null)

    with(TestContext()) {
        beforeSpec {
            tilgangsTjenesteForAnsatteMock.configureMock()
            mockOAuthServer.start()
        }
        afterSpec { mockOAuthServer.shutdown() }

        "POST /api/v2/tidslinjer" - {
            "alle hendelsestyper, fullt utfylt og med null-felter" {
                kontraktTestApplication { client ->
                    val response = client.hentViaPost(
                        "/api/v2/tidslinjer", mockOAuthServer.ansattToken(TestData.ansatt1), request
                    )
                    response.status shouldBe HttpStatusCode.OK
                    response.bodyAsText().skalVaereLikFasit("v2/tidslinjer-alle-hendelsestyper.json")
                    v2Mapper.readValue<TidslinjeResponse>(lesFasit("v2/tidslinjer-alle-hendelsestyper.json")) shouldBe
                            TidslinjeResponse(KontraktTestData.tidslinjer())
                }
            }
            "403 ProblemDetails når ansatt mangler tilgang" {
                kontraktTestApplication { client ->
                    val response = client.hentViaPost(
                        "/api/v2/tidslinjer", mockOAuthServer.ansattToken(TestData.ansatt3), request
                    )
                    response.status shouldBe HttpStatusCode.Forbidden
                    val normalisert = normaliserProblemDetails(response.bodyAsText())
                    normalisert.skalVaereLikFasit("v2/problemdetails-403-ingen-tilgang.json")
                    v2Mapper.readValue<ProblemDetails>(lesFasit("v2/problemdetails-403-ingen-tilgang.json")) shouldBe
                            v2Mapper.readValue<ProblemDetails>(normalisert)
                }
            }
        }

        "POST /api/v2/bekreftelser" - {
            "alle bekreftelsesstatuser" {
                kontraktTestApplication { client ->
                    val response = client.hentViaPost(
                        "/api/v2/bekreftelser", mockOAuthServer.ansattToken(TestData.ansatt1), request
                    )
                    response.status shouldBe HttpStatusCode.OK
                    response.bodyAsText().skalVaereLikFasit("v2/bekreftelser-alle-statuser.json")
                    val forventet = BekreftelserResponse(
                        KontraktTestData.tidslinjer()
                            .flatMap { tidslinje -> tidslinje.hendelser.mapNotNull { it.bekreftelseV1 } }
                            .sortedByDescending { it.bekreftelse?.svar?.sendtInnAv?.tidspunkt ?: Instant.EPOCH }
                    )
                    v2Mapper.readValue<BekreftelserResponse>(lesFasit("v2/bekreftelser-alle-statuser.json")) shouldBe
                            forventet
                }
            }
        }
    }
})
