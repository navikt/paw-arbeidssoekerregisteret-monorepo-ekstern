package no.nav.paw.arbeidssoekerregisteret.plugins

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import no.nav.paw.arbeidssoekerregisteret.context.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.routes.metricsRoutes
import no.nav.paw.health.model.HealthStatus

fun Application.configureRouting(applicationContext: ApplicationContext) {
    with(applicationContext) {
        routing {
            get("/internal/isAlive") {
                // TODO: health lib
                call.respondText(ContentType.Text.Plain, HttpStatusCode.OK) { HealthStatus.HEALTHY.value }
            }

            get("/internal/isReady") {
                call.respondText(ContentType.Text.Plain, HttpStatusCode.OK) { HealthStatus.HEALTHY.value }
            }
            metricsRoutes(prometheusMeterRegistry)
        }
    }
}
