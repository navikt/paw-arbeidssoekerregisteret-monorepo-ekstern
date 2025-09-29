package no.nav.paw.oppslagapi.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.ArbeidssoekerperiodeAggregertResponse
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.ArbeidssoekerperiodeRequest
import no.nav.paw.error.model.Data
import no.nav.paw.error.model.ProblemDetails
import no.nav.paw.error.model.Response
import no.nav.paw.error.model.map
import no.nav.paw.model.Identitetsnummer
import no.nav.paw.oppslagapi.data.query.ApplicationQueryLogic
import no.nav.paw.oppslagapi.data.query.gjeldeneEllerSisteTidslinje
import no.nav.paw.oppslagapi.v2TilV1.v1Bekreftelser
import no.nav.paw.oppslagapi.v2TilV1.v1OpplysningerAggregert
import no.nav.paw.oppslagapi.v2TilV1.v1Periode
import no.nav.paw.security.authentication.model.AzureAd
import no.nav.paw.security.authentication.model.Sluttbruker
import no.nav.paw.security.authentication.model.TokenX
import no.nav.paw.security.authentication.model.securityContext
import no.nav.paw.security.authentication.plugin.autentisering

const val V1_API_ARBEIDSSOEKERPERIODER_AGGREGERT = "arbeidssoekerperioder-aggregert"
const val V1_API_VEILEDER_ARBEIDSSOEKERPERIODER_AGGREGERT = "veileder/arbeidssoekerperioder-aggregert"

fun Route.v1PerioderAggregert(
    appQueryLogic: ApplicationQueryLogic
) {
    autentisering(issuer = TokenX) {
        get(V1_API_ARBEIDSSOEKERPERIODER_AGGREGERT) {
            val securityContext = call.securityContext()
            val bruker = (securityContext.bruker as? Sluttbruker)
                ?: throw IllegalArgumentException("Ugyldig token type, forventet Sluttbruker")
            val bareReturnerSiste = call.bareReturnerSiste()
            val response: Response<List<ArbeidssoekerperiodeAggregertResponse>> = appQueryLogic.hentTidslinjer(
                securityContext = securityContext,
                identitetsnummer = bruker.ident
            ).map {
                if (bareReturnerSiste) listOfNotNull(it.gjeldeneEllerSisteTidslinje())
                else it
            }.map { tidslinjer ->
                tidslinjer.map { tidslinje ->
                    val periode = tidslinje.v1Periode()
                    ArbeidssoekerperiodeAggregertResponse(
                        periodeId = periode.periodeId,
                        startet = periode.startet,
                        avsluttet = periode.avsluttet,
                        opplysningerOmArbeidssoeker = tidslinje.v1OpplysningerAggregert(),
                        bekreftelser = tidslinje.v1Bekreftelser()
                    )
                }
            }
            when (response) {
                is Data<List<ArbeidssoekerperiodeAggregertResponse>> -> {
                    call.respond(status = HttpStatusCode.OK, message = response.data)
                }

                is ProblemDetails -> {
                    call.respond(status = response.status, message = response)
                }
            }
        }
    }
}

fun Route.v1VeilederPerioderAggregert(
    appQueryLogic: ApplicationQueryLogic
) {
    autentisering(issuer = AzureAd) {
        post<ArbeidssoekerperiodeRequest>(V1_API_VEILEDER_ARBEIDSSOEKERPERIODER_AGGREGERT) { request ->
            val securityContext = call.securityContext()
            val identitetsnummer = Identitetsnummer(request.identitetsnummer)
            val bareReturnerSiste = call.bareReturnerSiste()
            val response = appQueryLogic.hentTidslinjer(
                securityContext = securityContext,
                identitetsnummer = identitetsnummer
            ).map {
                if (bareReturnerSiste) listOfNotNull(it.gjeldeneEllerSisteTidslinje())
                else it
            }.map { tidslinjer ->
                tidslinjer.map { tidslinje ->
                    val periode = tidslinje.v1Periode()
                    ArbeidssoekerperiodeAggregertResponse(
                        periodeId = periode.periodeId,
                        startet = periode.startet,
                        avsluttet = periode.avsluttet,
                        opplysningerOmArbeidssoeker = tidslinje.v1OpplysningerAggregert(),
                        bekreftelser = tidslinje.v1Bekreftelser()
                    )
                }
            }
            when (response) {
                is Data<List<ArbeidssoekerperiodeAggregertResponse>> -> {
                    call.respond(status = HttpStatusCode.OK, message = response.data)
                }

                is ProblemDetails -> {
                    call.respond(status = response.status, message = response)
                }
            }
        }
    }
}
