package no.nav.paw.arbeidssoekerregisteret.routes

import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import no.nav.paw.arbeidssoekerregisteret.context.ApplicationContext

fun Routing.metricsRoutes(applicationContext: ApplicationContext) {
    get("/internal/metrics") {
        call.respond(applicationContext.prometheusMeterRegistry.scrape())
    }
}