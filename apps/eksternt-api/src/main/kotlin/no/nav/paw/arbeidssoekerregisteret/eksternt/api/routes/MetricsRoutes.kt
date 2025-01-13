package no.nav.paw.arbeidssoekerregisteret.eksternt.api.routes

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

fun Route.metricsRoutes(prometheusMeterRegistry: PrometheusMeterRegistry) {
    get("/internal/metrics") {
        call.respond(prometheusMeterRegistry.scrape())
    }
}
