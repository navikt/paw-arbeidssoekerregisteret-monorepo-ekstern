package no.nav.paw.arbeidssoekerregisteret.plugins

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.config.HealthIndicator
import no.nav.paw.arbeidssoekerregisteret.routes.healthRoutes
import no.nav.paw.arbeidssoekerregisteret.routes.swaggerRoutes
import no.nav.paw.arbeidssoekerregisteret.routes.toggleRoutes

fun Application.configureRouting(
    healthIndicator: HealthIndicator,
    meterRegistry: PrometheusMeterRegistry
) {
    routing {
        healthRoutes(healthIndicator, meterRegistry)
        swaggerRoutes()
        toggleRoutes()
    }
}
