package no.nav.paw.arbeidssoekerregisteret.plugins

import io.ktor.server.application.Application
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import no.nav.paw.arbeidssoekerregisteret.context.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.egenvurderingRoutes
import no.nav.paw.health.livenessRoute
import no.nav.paw.health.readinessRoute
import no.nav.paw.health.startupRoute

fun Application.configureRouting(applicationContext: ApplicationContext) {
    with(applicationContext) {
        routing {
            startupRoute(applicationContext.healthChecks)
            livenessRoute(applicationContext.healthChecks)
            readinessRoute(applicationContext.healthChecks)

            get("/internal/metrics") {
                call.respond<String>(prometheusMeterRegistry.scrape())
            }
            swaggerUI(path = "/api/docs")
            egenvurderingRoutes(applicationContext.egenvurderingService)
        }
    }
}
