package no.nav.paw.arbeidssoekerregisteret.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.paw.arbeidssoekerregisteret.context.ConfigContext
import no.nav.paw.arbeidssoekerregisteret.context.LoggingContext
import no.nav.paw.arbeidssoekerregisteret.exception.AuthorizationException
import no.nav.paw.arbeidssoekerregisteret.model.Toggle
import no.nav.paw.arbeidssoekerregisteret.model.ToggleAction
import no.nav.paw.arbeidssoekerregisteret.model.ToggleRequest
import no.nav.paw.arbeidssoekerregisteret.model.getPid
import no.nav.paw.arbeidssoekerregisteret.routes.auth.resolveClaims
import no.nav.paw.arbeidssoekerregisteret.service.ToggleService

context(ConfigContext, LoggingContext)
fun Route.toggleRoutes(toggleService: ToggleService) {
    authenticate("tokenx") {
        post<ToggleRequest>("/api/v1/microfrontend-toggle") { toggleRequest ->
            if (toggleRequest.action == ToggleAction.ENABLE) {
                throw AuthorizationException(
                    "OPERASJON_IKKE_TILLATT",
                    "Det er ikke tillatt å aktivere microfrontends via dette endepunktet"
                )
            }

            // TODO Sjekke at det kun er TokenX bearer token?
            val claims = call.resolveClaims()
            val identitetsnummer = claims?.getPid() ?: throw AuthorizationException(
                "UFULLSTENDING_BEARER_TOKEN",
                "Bearer token inneholder ikke 'pid' claim"
            )

            toggleRequest.let {
                val toggle = toggleService.sendToggle(it.action, identitetsnummer, it.microfrontendId)
                call.respond<Toggle>(HttpStatusCode.Accepted, toggle)
            }
        }
    }
}
