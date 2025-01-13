package no.nav.paw.arbeidssoekerregisteret.eksternt.api.plugins

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.routes.metricsRoutes
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.routes.periodeRoutes
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.routes.swaggerRoutes
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.services.PeriodeService
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.health.route.healthRoutes

fun Application.configureRouting(
    healthIndicatorRepository: HealthIndicatorRepository,
    meterRegistry: PrometheusMeterRegistry,
    periodeService: PeriodeService
) {
    routing {
        healthRoutes(healthIndicatorRepository)
        metricsRoutes(meterRegistry)
        swaggerRoutes()
        periodeRoutes(periodeService)
    }
}
