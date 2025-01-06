package no.nav.paw.arbeidssoekerregisteret.eksternt.api.plugins

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.routes.periodeRoutes
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.routes.healthRoutes
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.routes.swaggerRoutes
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.services.PeriodeService

fun Application.configureRouting(
    meterRegistry: PrometheusMeterRegistry,
    periodeService: PeriodeService
) {
    routing {
        healthRoutes(meterRegistry)
        swaggerRoutes()
        periodeRoutes(periodeService)
    }
}
