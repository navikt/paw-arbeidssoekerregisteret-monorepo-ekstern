package no.nav.paw.oppslagapi.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.ApiV2BekreftelserPostRequest
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.TidslinjeResponse
import no.nav.paw.error.model.Data
import no.nav.paw.error.model.ProblemDetails
import no.nav.paw.error.model.map
import no.nav.paw.oppslagapi.data.query.ApplicationQueryLogic
import no.nav.paw.security.authentication.model.AzureAd
import no.nav.paw.security.authentication.model.TokenX
import no.nav.paw.security.authentication.model.securityContext
import no.nav.paw.security.authentication.plugin.autentisering

fun Route.v2Tidslinjer(appQueryLogic: ApplicationQueryLogic) {
    autentisering(TokenX, AzureAd) {
        post<ApiV2BekreftelserPostRequest> { request ->
            val securityContext = call.securityContext()
            val response = appQueryLogic.lagTidslinjer(
                securityContext = securityContext,
                request = request
            ).map(::TidslinjeResponse)
            when (response) {
                is Data<TidslinjeResponse> -> {
                    call.respond(HttpStatusCode.Companion.OK, response.data)
                }

                is ProblemDetails -> {
                    call.respond(response.status, response)
                }
            }
        }
    }
}