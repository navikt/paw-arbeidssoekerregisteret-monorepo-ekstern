package no.nav.paw.arbeidssoekerregisteret.plugins

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import no.nav.paw.arbeidssoekerregisteret.context.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.routes.metricsRoutes
import no.nav.paw.arbeidssoekerregisteret.routes.swaggerRoutes
import no.nav.paw.arbeidssoekerregisteret.routes.toggleRoutes
import no.nav.paw.health.route.healthRoutes

fun Application.configureRouting(applicationContext: ApplicationContext) {
    routing {
        healthRoutes(applicationContext.healthIndicatorRepository)
        metricsRoutes(applicationContext)
        swaggerRoutes()
        toggleRoutes(applicationContext)
    }
}
