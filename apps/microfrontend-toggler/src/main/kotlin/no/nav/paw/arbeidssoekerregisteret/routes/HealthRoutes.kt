package no.nav.paw.arbeidssoekerregisteret.routes

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.config.HealthIndicator
import no.nav.paw.arbeidssoekerregisteret.config.HealthStatus

fun Route.healthRoutes(
    healthIndicator: HealthIndicator,
    meterRegistry: PrometheusMeterRegistry
) {

    // TODO Koble Kafka Stream state mot helsesjekk
    get("/internal/isAlive") {
        call.respondText("ALIVE", ContentType.Text.Plain)
    }

    get("/internal/isReady") {
        val status = healthIndicator.getStatus()
        when (status) {
            HealthStatus.HEALTHY -> call.respondText(
                ContentType.Text.Plain,
                HttpStatusCode.OK
            ) { status.text }

            else -> call.respondText(
                ContentType.Text.Plain,
                HttpStatusCode.ServiceUnavailable
            ) { status.text }
        }
    }

    get("/internal/metrics") {
        call.respond(meterRegistry.scrape())
    }
}
