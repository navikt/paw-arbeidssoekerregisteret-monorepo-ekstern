package no.nav.paw.arbeidssoekerregisteret.plugins

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import no.nav.paw.arbeidssoekerregisteret.context.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.routes.metricsRoutes
import no.nav.paw.health.livenessRoute
import no.nav.paw.health.readinessRoute

fun Application.configureRouting(applicationContext: ApplicationContext) {
    with(applicationContext) {
        routing {
            livenessRoute(healthChecks)
            readinessRoute(healthChecks)
            metricsRoutes(prometheusMeterRegistry)
        }
    }
}
