package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.plugins

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.context.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.routes.metricsRoutes
import no.nav.paw.health.liveness.livenessRoute
import no.nav.paw.health.probes.databaseIsAliveCheck
import no.nav.paw.health.readiness.readinessRoute
import no.nav.paw.health.startup.startupRoute

fun Application.configureRouting(applicationContext: ApplicationContext) {
    with(applicationContext) {
        routing {
            startupRoute(healthChecks)
            livenessRoute(healthChecks)
            readinessRoute(healthChecks)
            metricsRoutes(prometheusMeterRegistry)
        }
    }
}
