package no.nav.paw.oppslagapi.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.ArbeidssoekerperiodeRequest
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.ArbeidssoekerperiodeResponse
import no.nav.paw.error.model.Data
import no.nav.paw.error.model.ProblemDetails
import no.nav.paw.error.model.Response
import no.nav.paw.error.model.map
import no.nav.paw.model.Identitetsnummer
import no.nav.paw.oppslagapi.data.query.ApplicationQueryLogic
import no.nav.paw.oppslagapi.data.query.gjeldeneEllerSisteTidslinje
import no.nav.paw.oppslagapi.v2TilV1.v1Metadata
import no.nav.paw.security.authentication.model.AzureAd
import no.nav.paw.security.authentication.model.SecurityContext
import no.nav.paw.security.authentication.model.Sluttbruker
import no.nav.paw.security.authentication.model.TokenX
import no.nav.paw.security.authentication.model.securityContext
import no.nav.paw.security.authentication.plugin.autentisering

const val V1_API_ARBEIDSSOEKERPERIODER = "arbeidssoekerperioder"
const val V1_API_VEILEDER_ARBEIDSSOEKERPERIODER = "veileder/arbeidssoekerperioder"

fun Route.v1Perioder(
    appQueryLogic: ApplicationQueryLogic
) {
    autentisering(issuer = TokenX) {
        get(V1_API_ARBEIDSSOEKERPERIODER) {
            val securityContext = call.securityContext()
            val bruker = (securityContext.bruker as? Sluttbruker)
                ?: throw IllegalArgumentException("Ugyldig token type, forventet Sluttbruker")
            prosesserHentPeriodeV1Kall(
                appQueryLogic = appQueryLogic,
                securityContext = securityContext,
                identitetsnummer = bruker.ident
            )
        }
    }
}

fun Route.v1VeilederPerioder(
    appQueryLogic: ApplicationQueryLogic
) {
    autentisering(issuer = AzureAd) {
        post<ArbeidssoekerperiodeRequest>(V1_API_VEILEDER_ARBEIDSSOEKERPERIODER) { request ->
            val securityContext = call.securityContext()
            val identitetsnummer = Identitetsnummer(request.identitetsnummer)
            prosesserHentPeriodeV1Kall(
                appQueryLogic = appQueryLogic,
                securityContext = securityContext,
                identitetsnummer = identitetsnummer
            )
        }
    }
}

private suspend fun RoutingContext.prosesserHentPeriodeV1Kall(
    appQueryLogic: ApplicationQueryLogic,
    securityContext: SecurityContext,
    identitetsnummer: Identitetsnummer
) {
    val bareReturnerSiste = call.bareReturnerSiste()
    val response: Response<List<ArbeidssoekerperiodeResponse>> = appQueryLogic.hentTidslinjer(
        securityContext = securityContext,
        identitetsnummer = identitetsnummer
    ).map { tidslinjer ->
        if (bareReturnerSiste) {
            listOfNotNull(tidslinjer.gjeldeneEllerSisteTidslinje())
        } else {
            tidslinjer
        }
    }.map { tidslinjer ->
            tidslinjer.map {
                ArbeidssoekerperiodeResponse(
                    periodeId = it.periodeId,
                    startet = it.hendelser.firstNotNullOf { hendelse -> hendelse.periodeStartetV1 }.v1Metadata(),
                    avsluttet = it.hendelser.firstNotNullOfOrNull { hendelse -> hendelse.periodeAvsluttetV1 }
                        ?.v1Metadata()
                )
            }
        }

    when (response) {
        is Data<List<ArbeidssoekerperiodeResponse>> -> {
            call.respond(status = HttpStatusCode.OK, message = response.data)
        }

        is ProblemDetails -> {
            call.respond(status = response.status, message = response)
        }
    }
}
