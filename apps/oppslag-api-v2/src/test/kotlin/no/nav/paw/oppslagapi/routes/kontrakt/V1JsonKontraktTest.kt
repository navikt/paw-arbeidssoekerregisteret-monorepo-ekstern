package no.nav.paw.oppslagapi.routes.kontrakt

import tools.jackson.module.kotlin.jacksonMapperBuilder
import tools.jackson.module.kotlin.readValue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.ArbeidssoekerperiodeAggregertResponse
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.ArbeidssoekerperiodeRequest
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.ArbeidssoekerperiodeResponse
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.BekreftelseResponse
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.OpplysningerOmArbeidssoekerRequest
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.OpplysningerOmArbeidssoekerResponse
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.ProfileringRequest
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.ProfileringResponse
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.SamletInformasjonResponse
import no.nav.paw.error.model.ProblemDetails
import no.nav.paw.oppslagapi.mapping.v1.v1Bekreftelser
import no.nav.paw.oppslagapi.mapping.v1.v1Opplysninger
import no.nav.paw.oppslagapi.mapping.v1.v1OpplysningerAggregert
import no.nav.paw.oppslagapi.mapping.v1.v1Periode
import no.nav.paw.oppslagapi.mapping.v1.v1Profileringer
import no.nav.paw.oppslagapi.test.TestContext
import no.nav.paw.oppslagapi.test.TestData
import no.nav.paw.oppslagapi.test.ansattToken
import no.nav.paw.oppslagapi.test.configureMock
import no.nav.paw.oppslagapi.test.hentViaPost
import no.nav.paw.oppslagapi.utils.configureJacksonForV1

/**
 * JSON-kontrakttester for /api/v1.
 *
 * Fasitfilene i `src/test/resources/json-kontrakt/api/v1/` må aldri endres; de låser formatet vi sender
 * til konsumenter. Responsen kommer fra de ekte rutene med ekte ContentNegotiation (configureJacksonForV1).
 * Vi bruker veileder-variantene (POST /api/v1/veileder/...). Bruker-variantene (GET) bruker de samme
 * mapperne og responstypene, og har derfor samme format.
 * Hver test sjekker at (1) responsen er lik fasitfilen, og (2) at fasitfilen leses inn til forventet objekt
 * med samme produksjonsmapper.
 */
class V1JsonKontraktTest : FreeSpec({
    val v1Mapper = jacksonMapperBuilder().configureJacksonForV1().build()
    val ident = KontraktTestData.identitetsnummer.value
    val periodeRequest = ArbeidssoekerperiodeRequest(identitetsnummer = ident)
    val tidslinjer = KontraktTestData.tidslinjer()

    with(TestContext()) {
        beforeSpec {
            tilgangsTjenesteForAnsatteMock.configureMock()
            mockOAuthServer.start()
        }
        afterSpec { mockOAuthServer.shutdown() }

        "POST /api/v1/veileder/arbeidssoekerperioder" - {
            "avsluttet og aktiv periode" {
                kontraktTestApplication { client ->
                    val response = client.hentViaPost(
                        "/api/v1/veileder/arbeidssoekerperioder",
                        mockOAuthServer.ansattToken(TestData.ansatt1),
                        periodeRequest
                    )
                    response.status shouldBe HttpStatusCode.OK
                    response.bodyAsText().skalVaereLikFasit("v1/arbeidssoekerperioder.json")
                    v1Mapper.readValue<List<ArbeidssoekerperiodeResponse>>(lesFasit("v1/arbeidssoekerperioder.json")) shouldBe
                            tidslinjer.map { it.v1Periode() }
                }
            }
            "403 ProblemDetails når ansatt mangler tilgang" {
                kontraktTestApplication { client ->
                    val response = client.hentViaPost(
                        "/api/v1/veileder/arbeidssoekerperioder",
                        mockOAuthServer.ansattToken(TestData.ansatt3),
                        periodeRequest
                    )
                    response.status shouldBe HttpStatusCode.Forbidden
                    val normalisert = normaliserProblemDetails(response.bodyAsText())
                    normalisert.skalVaereLikFasit("v1/problemdetails-403-ingen-tilgang.json")
                    v1Mapper.readValue<ProblemDetails>(lesFasit("v1/problemdetails-403-ingen-tilgang.json")) shouldBe
                            v1Mapper.readValue<ProblemDetails>(normalisert)
                }
            }
        }

        "POST /api/v1/veileder/profilering" {
            kontraktTestApplication { client ->
                val response = client.hentViaPost(
                    "/api/v1/veileder/profilering",
                    mockOAuthServer.ansattToken(TestData.ansatt1),
                    ProfileringRequest(identitetsnummer = ident)
                )
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText().skalVaereLikFasit("v1/profilering.json")
                v1Mapper.readValue<List<ProfileringResponse>>(lesFasit("v1/profilering.json")) shouldBe
                        tidslinjer.flatMap { it.v1Profileringer() }
            }
        }

        "POST /api/v1/veileder/arbeidssoekerperioder-aggregert" {
            kontraktTestApplication { client ->
                val response = client.hentViaPost(
                    "/api/v1/veileder/arbeidssoekerperioder-aggregert",
                    mockOAuthServer.ansattToken(TestData.ansatt1),
                    periodeRequest
                )
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText().skalVaereLikFasit("v1/arbeidssoekerperioder-aggregert.json")
                val forventet = tidslinjer.map { tidslinje ->
                    val periode = tidslinje.v1Periode()
                    ArbeidssoekerperiodeAggregertResponse(
                        periodeId = periode.periodeId,
                        startet = periode.startet,
                        avsluttet = periode.avsluttet,
                        opplysningerOmArbeidssoeker = tidslinje.v1OpplysningerAggregert(),
                        bekreftelser = tidslinje.v1Bekreftelser()
                    )
                }
                v1Mapper.readValue<List<ArbeidssoekerperiodeAggregertResponse>>(
                    lesFasit("v1/arbeidssoekerperioder-aggregert.json")
                ) shouldBe forventet
            }
        }

        "POST /api/v1/veileder/opplysninger-om-arbeidssoeker" {
            kontraktTestApplication { client ->
                val response = client.hentViaPost(
                    "/api/v1/veileder/opplysninger-om-arbeidssoeker",
                    mockOAuthServer.ansattToken(TestData.ansatt1),
                    OpplysningerOmArbeidssoekerRequest(identitetsnummer = ident)
                )
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText().skalVaereLikFasit("v1/opplysninger-om-arbeidssoeker.json")
                v1Mapper.readValue<List<OpplysningerOmArbeidssoekerResponse>>(
                    lesFasit("v1/opplysninger-om-arbeidssoeker.json")
                ) shouldBe tidslinjer.flatMap { it.v1Opplysninger() }
            }
        }

        "POST /api/v1/veileder/arbeidssoekerbekreftelser" {
            kontraktTestApplication { client ->
                val response = client.hentViaPost(
                    "/api/v1/veileder/arbeidssoekerbekreftelser",
                    mockOAuthServer.ansattToken(TestData.ansatt1),
                    OpplysningerOmArbeidssoekerRequest(identitetsnummer = ident)
                )
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText().skalVaereLikFasit("v1/arbeidssoekerbekreftelser.json")
                v1Mapper.readValue<List<BekreftelseResponse>>(lesFasit("v1/arbeidssoekerbekreftelser.json")) shouldBe
                        tidslinjer.flatMap { it.v1Bekreftelser() }
            }
        }

        "POST /api/v1/veileder/samlet-informasjon" {
            kontraktTestApplication { client ->
                val response = client.hentViaPost(
                    "/api/v1/veileder/samlet-informasjon",
                    mockOAuthServer.ansattToken(TestData.ansatt1),
                    periodeRequest
                )
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText().skalVaereLikFasit("v1/samlet-informasjon.json")
                // Samme sortering som tilSamletInformasjon (siste=false)
                val forventet = SamletInformasjonResponse(
                    arbeidssoekerperioder = tidslinjer.map { it.v1Periode() },
                    opplysningerOmArbeidssoeker = tidslinjer.flatMap { t ->
                        t.v1Opplysninger().sortedByDescending { it.sendtInnAv.tidspunkt }
                    },
                    profilering = tidslinjer.flatMap { t ->
                        t.v1Profileringer().sortedByDescending { it.sendtInnAv.tidspunkt }
                    },
                    bekreftelser = tidslinjer.flatMap { t ->
                        t.v1Bekreftelser().sortedByDescending { it.svar.gjelderTil }
                    }
                )
                v1Mapper.readValue<SamletInformasjonResponse>(lesFasit("v1/samlet-informasjon.json")) shouldBe
                        forventet
            }
        }
    }
})
