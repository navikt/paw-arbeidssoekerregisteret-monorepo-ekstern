package no.nav.paw.oppslagapi.health

import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

fun <A> Routing.internalRoutes(
    healthIndicator: A,
    prometheusRegistry: PrometheusMeterRegistry
) where A : IsAlive, A : IsReady, A : HasStarted {
    route("internal") {
        route("isAlive") {
            get {
                val (code, message) = healthIndicator.isAlive().httpResponse()
                call.respondText(text = message, status = code)
            }
        }
        route("isReady") {
            get {
                val (code, message) = healthIndicator.isReady().httpResponse()
                call.respondText(text = message, status = code)
            }
        }
        route("hasStarted") {
            get {
                val (code, message) = healthIndicator.hasStarted().httpResponse()
                call.respondText(text = message, status = code)
            }
        }
        route("metrics") {
            get {
                call.respondText(prometheusRegistry.scrape())
            }
        }
    }
}