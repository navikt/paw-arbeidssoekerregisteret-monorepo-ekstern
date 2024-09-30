package no.nav.paw.arbeidssoekerregisteret.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.paw.arbeidssoekerregisteret.context.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.context.resolveRequest
import no.nav.paw.arbeidssoekerregisteret.exception.BrukerHarIkkeTilgangException
import no.nav.paw.arbeidssoekerregisteret.model.Toggle
import no.nav.paw.arbeidssoekerregisteret.model.ToggleAction
import no.nav.paw.arbeidssoekerregisteret.model.ToggleRequest
import no.nav.paw.arbeidssoekerregisteret.model.buildToggle

fun Route.toggleRoutes(applicationContext: ApplicationContext) {
    val authorizationService = applicationContext.authorizationService
    val toggleService = applicationContext.toggleService

    route("/api/v1") {
        authenticate("tokenx") {
            post<ToggleRequest>("/microfrontend-toggle") { toggleRequest ->
                val requestContext = resolveRequest()
                val securityContext = authorizationService.authorize(requestContext)

                if (toggleRequest.action == ToggleAction.ENABLE) {
                    throw BrukerHarIkkeTilgangException(
                        "Det er ikke tillatt å aktivere microfrontends via dette endepunktet"
                    )
                }


                val toggle = toggleRequest.buildToggle(securityContext.innloggetBruker.ident)
                toggleService.sendToggle(toggle)
                call.respond<Toggle>(HttpStatusCode.Accepted, toggle)
            }
        }
    }
}
