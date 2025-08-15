package no.nav.paw.oppslagapi.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.ArbeidssoekerperiodeResponse
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.ApiV2BekreftelserPostRequest
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.BekreftelserResponse
import no.nav.paw.error.model.Data
import no.nav.paw.error.model.ProblemDetails
import no.nav.paw.error.model.Response
import no.nav.paw.oppslagapi.data.query.ApplicationQueryLogic
import no.nav.paw.security.authentication.model.AzureAd
import no.nav.paw.security.authentication.model.Sluttbruker
import no.nav.paw.security.authentication.model.TokenX
import no.nav.paw.security.authentication.model.securityContext
import no.nav.paw.security.authentication.plugin.autentisering

fun Route.v1Perioder(
    appQueryLogic: ApplicationQueryLogic
) {
    autentisering(TokenX, ) {
        get("arbeidssoekerperioder") {
            val securityContext = call.securityContext()
            val bruker = (securityContext.bruker as? Sluttbruker) ?:
                throw IllegalArgumentException("Ugyldig token type, forventet Sluttbruker")
            val response: Response<List<ArbeidssoekerperiodeResponse>> = appQueryLogic.hentPerioder(
                securityContext = securityContext,
                identitetsnummer = bruker.ident
            )
            when (response) {
                is Data<List<ArbeidssoekerperiodeResponse>> -> {
                    call.respond(HttpStatusCode.Companion.OK, response.data)
                }

                is ProblemDetails -> {
                    call.respond(response.status, response)
                }
            }
        }
    }
}