package no.nav.paw.arbeidssoekerregisteret.plugins

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import no.nav.paw.arbeidssoekerregisteret.context.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.routes.egenvurderingRoutes
import no.nav.paw.arbeidssoekerregisteret.routes.metricsRoutes
import no.nav.paw.arbeidssoekerregisteret.routes.swaggerRoutes
import no.nav.paw.health.liveness.livenessRoute
import no.nav.paw.health.probes.databaseIsAliveCheck
import no.nav.paw.health.readiness.readinessRoute
import no.nav.paw.health.startup.startupRoute

fun Application.configureRouting(applicationContext: ApplicationContext) {
    with(applicationContext) {
        routing {
            startupRoute(applicationContext.healthChecks)
            livenessRoute(applicationContext.healthChecks)
            readinessRoute(applicationContext.healthChecks)
            metricsRoutes(prometheusMeterRegistry)
            swaggerRoutes()
            egenvurderingRoutes(applicationContext.authorizationService, applicationContext.egenvurderingService)
        }
    }
}
