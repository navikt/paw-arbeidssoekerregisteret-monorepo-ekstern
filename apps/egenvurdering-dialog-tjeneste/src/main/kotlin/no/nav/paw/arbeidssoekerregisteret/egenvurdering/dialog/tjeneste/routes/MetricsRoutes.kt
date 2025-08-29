package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.routes

import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

fun Routing.metricsRoutes(meterRegistry: PrometheusMeterRegistry) {
    get("/internal/metrics") {
        call.respond(meterRegistry.scrape())
    }
}