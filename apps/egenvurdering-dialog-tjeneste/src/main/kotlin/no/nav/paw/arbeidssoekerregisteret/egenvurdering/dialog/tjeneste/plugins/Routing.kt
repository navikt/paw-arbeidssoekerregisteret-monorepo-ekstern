package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.plugins

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.route.egenvurderingRoutes
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.route.openApiRoutes
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.service.DialogService
import no.nav.paw.health.HealthChecks
import no.nav.paw.health.livenessRoute
import no.nav.paw.health.readinessRoute
import no.nav.paw.health.startupRoute
import no.nav.paw.metrics.route.metricsRoutes

fun Application.configureRouting(
    healthChecks: HealthChecks,
    meterRegistry: PrometheusMeterRegistry,
    dialogService: DialogService
) {
    routing {
        startupRoute(healthChecks)
        livenessRoute(healthChecks)
        readinessRoute(healthChecks)
        metricsRoutes(meterRegistry)
        openApiRoutes()
        egenvurderingRoutes(dialogService)
    }
}
