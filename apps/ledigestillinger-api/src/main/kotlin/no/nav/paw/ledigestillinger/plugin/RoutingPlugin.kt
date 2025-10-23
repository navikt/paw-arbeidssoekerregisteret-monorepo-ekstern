package no.nav.paw.ledigestillinger.plugin

import io.ktor.server.application.Application
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.routing
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.health.HealthChecks
import no.nav.paw.health.livenessRoute
import no.nav.paw.health.readinessRoute
import no.nav.paw.health.startupRoute
import no.nav.paw.ledigestillinger.route.stillingRoutes
import no.nav.paw.ledigestillinger.service.StillingService
import no.nav.paw.metrics.route.metricsRoutes

fun Application.configureRouting(
    healthChecks: HealthChecks,
    meterRegistry: PrometheusMeterRegistry,
    stillingService: StillingService
) {
    routing {
        livenessRoute(healthChecks)
        readinessRoute(healthChecks)
        startupRoute(healthChecks)
        metricsRoutes(meterRegistry)
        swaggerUI(path = "docs", swaggerFile = "openapi/documentation.yaml")
        stillingRoutes(stillingService)
    }
}