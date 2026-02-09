package no.nav.paw.arbeidssoekerregisteret.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.paw.arbeidssoekerregisteret.model.Sensitivitet
import no.nav.paw.arbeidssoekerregisteret.model.Toggle
import no.nav.paw.arbeidssoekerregisteret.model.ToggleAction
import no.nav.paw.arbeidssoekerregisteret.model.ToggleRequest
import no.nav.paw.arbeidssoekerregisteret.service.AuthorizationService
import no.nav.paw.arbeidssoekerregisteret.service.ToggleService
import no.nav.paw.security.authentication.model.Sluttbruker
import no.nav.paw.security.authentication.model.TokenX
import no.nav.paw.security.authentication.model.bruker
import no.nav.paw.security.authentication.plugin.autentisering
import no.nav.paw.security.authorization.exception.IngenTilgangException
import no.nav.paw.security.authorization.interceptor.autorisering
import no.nav.paw.security.authorization.model.Action

fun Route.toggleRoutes(
    authorizationService: AuthorizationService,
    toggleService: ToggleService
) {

    route("/api/v1") {
        autentisering(TokenX) {
            post<ToggleRequest>("/microfrontend-toggle") { request ->
                val accessPolicies = authorizationService.accessPolicies()
                autorisering(Action.WRITE, accessPolicies) {

                    if (request.action == ToggleAction.ENABLE) {
                        throw IngenTilgangException("Det er ikke tillatt å aktivere microfrontends via dette endepunktet")
                    }

                    val bruker = call.bruker<Sluttbruker>()
                    val toggle = request.asToggle(
                        bruker.ident,
                        Sensitivitet.HIGH // TODO Ikke i bruk for DISABLE
                    )
                    toggleService.sendToggle(toggle)
                    call.respond<Toggle>(HttpStatusCode.Accepted, toggle)
                }
            }
        }
    }
}
