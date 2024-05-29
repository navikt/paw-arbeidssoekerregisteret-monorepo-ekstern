package no.nav.paw.arbeidssoekerregisteret.plugins

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.config.HealthIndicator
import no.nav.paw.arbeidssoekerregisteret.context.ConfigContext
import no.nav.paw.arbeidssoekerregisteret.routes.healthRoutes
import no.nav.paw.arbeidssoekerregisteret.routes.swaggerRoutes
import no.nav.paw.arbeidssoekerregisteret.routes.toggleRoutes
import no.nav.paw.arbeidssoekerregisteret.service.ToggleService

context(ConfigContext)
fun Application.configureRouting(
    healthIndicator: HealthIndicator,
    meterRegistry: PrometheusMeterRegistry,
    toggleService: ToggleService
) {
    routing {
        healthRoutes(healthIndicator, meterRegistry)
        swaggerRoutes()
        toggleRoutes(toggleService)
    }
}
