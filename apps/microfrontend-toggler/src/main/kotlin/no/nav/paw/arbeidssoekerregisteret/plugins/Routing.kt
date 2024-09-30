package no.nav.paw.arbeidssoekerregisteret.plugins

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.routes.healthRoutes
import no.nav.paw.arbeidssoekerregisteret.routes.swaggerRoutes
import no.nav.paw.arbeidssoekerregisteret.routes.toggleRoutes
import no.nav.paw.arbeidssoekerregisteret.service.HealthIndicatorService
import no.nav.paw.arbeidssoekerregisteret.service.ToggleService

fun Application.configureRouting(
    healthIndicatorService: HealthIndicatorService,
    meterRegistry: PrometheusMeterRegistry,
    toggleService: ToggleService
) {
    routing {
        healthRoutes(healthIndicatorService, meterRegistry)
        swaggerRoutes()
        toggleRoutes(toggleService)
    }
}
