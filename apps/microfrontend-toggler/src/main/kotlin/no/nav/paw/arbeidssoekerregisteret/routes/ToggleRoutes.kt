package no.nav.paw.arbeidssoekerregisteret.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.paw.arbeidssoekerregisteret.context.ConfigContext
import no.nav.paw.arbeidssoekerregisteret.context.LoggingContext
import no.nav.paw.arbeidssoekerregisteret.model.Toggle
import no.nav.paw.arbeidssoekerregisteret.model.ToggleRequest
import no.nav.paw.arbeidssoekerregisteret.service.ToggleService

context(ConfigContext, LoggingContext)
fun Route.toggleRoutes(toggleService: ToggleService) {
    post<ToggleRequest>("/api/v1/microfrontend-toggle") { toggleRequest ->
        val toggle = toggleService.processToggle(toggleRequest)
        call.respond<Toggle>(HttpStatusCode.Accepted, toggle)
    }
}
