package no.nav.paw.ledigestillinger.plugin

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.api.docs.routes.apiDocsRoutes
import no.nav.paw.health.HealthChecks
import no.nav.paw.health.livenessRoute
import no.nav.paw.health.readinessRoute
import no.nav.paw.health.startupRoute
import no.nav.paw.ledigestillinger.route.stillingRoutes
import no.nav.paw.ledigestillinger.service.StillingService
import no.nav.paw.ledigestillinger.service.StillingServiceV2
import no.nav.paw.metrics.route.metricsRoutes

fun Application.configureRouting(
    healthChecks: HealthChecks,
    meterRegistry: PrometheusMeterRegistry,
    stillingService: StillingServiceV2
) {
    routing {
        livenessRoute(healthChecks)
        readinessRoute(healthChecks)
        startupRoute(healthChecks)
        metricsRoutes(meterRegistry)
        apiDocsRoutes()
        stillingRoutes(stillingService)
    }
}