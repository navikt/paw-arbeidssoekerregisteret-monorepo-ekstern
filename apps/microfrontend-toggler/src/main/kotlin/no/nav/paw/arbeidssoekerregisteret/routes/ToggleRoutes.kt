package no.nav.paw.arbeidssoekerregisteret.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.paw.arbeidssoekerregisteret.context.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.model.Toggle
import no.nav.paw.arbeidssoekerregisteret.model.ToggleAction
import no.nav.paw.arbeidssoekerregisteret.model.ToggleRequest
import no.nav.paw.arbeidssoekerregisteret.model.buildToggle
import no.nav.paw.security.authentication.interceptor.autentisering
import no.nav.paw.security.authentication.model.Sluttbruker
import no.nav.paw.security.authentication.model.TokenX
import no.nav.paw.security.authentication.model.bruker
import no.nav.paw.security.authorization.exception.IngenTilgangException
import no.nav.paw.security.authorization.interceptor.autorisering
import no.nav.paw.security.authorization.model.Action

fun Route.toggleRoutes(applicationContext: ApplicationContext) {
    val authorizationService = applicationContext.authorizationService
    val toggleService = applicationContext.toggleService

    route("/api/v1") {
        autentisering(TokenX) {
            post<ToggleRequest>("/microfrontend-toggle") { toggleRequest ->
                val accessPolicies = authorizationService.accessPolicies()
                autorisering(Action.WRITE, accessPolicies) {

                    if (toggleRequest.action == ToggleAction.ENABLE) {
                        throw IngenTilgangException("Det er ikke tillatt å aktivere microfrontends via dette endepunktet")
                    }

                    val bruker = call.bruker<Sluttbruker>()
                    val toggle = toggleRequest.buildToggle(bruker.ident)
                    toggleService.sendToggle(toggle)
                    call.respond<Toggle>(HttpStatusCode.Accepted, toggle)
                }
            }
        }
    }
}
