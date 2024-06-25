package no.nav.paw.arbeidssoekerregisteret.plugins

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.context.ConfigContext
import no.nav.paw.arbeidssoekerregisteret.context.LoggingContext
import no.nav.paw.arbeidssoekerregisteret.routes.healthRoutes
import no.nav.paw.arbeidssoekerregisteret.routes.swaggerRoutes
import no.nav.paw.arbeidssoekerregisteret.routes.toggleRoutes
import no.nav.paw.arbeidssoekerregisteret.service.HealthIndicatorService
import no.nav.paw.arbeidssoekerregisteret.service.ToggleService

context(ConfigContext, LoggingContext)
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
