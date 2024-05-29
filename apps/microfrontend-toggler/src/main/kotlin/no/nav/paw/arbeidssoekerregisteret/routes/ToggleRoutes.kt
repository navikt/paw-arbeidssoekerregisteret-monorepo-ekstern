package no.nav.paw.arbeidssoekerregisteret.routes

import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.toggleRoutes() {
    get("/api/v1/microfrontend-toggle") {
        call.respond("toggle") // TODO Implementer logikk
    }
}
