package no.nav.paw.arbeidssoekerregisteret.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.paw.arbeidssoekerregisteret.exception.OperasjonIkkeTillattException
import no.nav.paw.arbeidssoekerregisteret.exception.UfullstendigBearerTokenException
import no.nav.paw.arbeidssoekerregisteret.model.Toggle
import no.nav.paw.arbeidssoekerregisteret.model.ToggleAction
import no.nav.paw.arbeidssoekerregisteret.model.ToggleRequest
import no.nav.paw.arbeidssoekerregisteret.model.buildToggle
import no.nav.paw.arbeidssoekerregisteret.model.getPid
import no.nav.paw.arbeidssoekerregisteret.routes.auth.resolveClaims
import no.nav.paw.arbeidssoekerregisteret.service.ToggleService

fun Route.toggleRoutes(toggleService: ToggleService) {
    authenticate("tokenx") {
        post<ToggleRequest>("/api/v1/microfrontend-toggle") { toggleRequest ->
            if (toggleRequest.action == ToggleAction.ENABLE) {
                throw OperasjonIkkeTillattException(
                    "Det er ikke tillatt å aktivere microfrontends via dette endepunktet"
                )
            }

            // TODO Sjekke at det kun er TokenX bearer token?
            val claims = call.resolveClaims()
            val identitetsnummer = claims?.getPid() ?: throw UfullstendigBearerTokenException(
                "Bearer token inneholder ikke 'pid' claim"
            )


            val toggle = toggleRequest.buildToggle(identitetsnummer)
            toggleService.sendToggle(toggle)
            call.respond<Toggle>(HttpStatusCode.Accepted, toggle)
        }
    }
}
