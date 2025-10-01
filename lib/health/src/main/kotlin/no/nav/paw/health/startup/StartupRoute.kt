package no.nav.paw.health.startup

import io.ktor.http.ContentType.Text
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.HttpStatusCode.Companion.ServiceUnavailable
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.paw.health.HealthStatus.HEALTHY
import no.nav.paw.health.HealthStatus.UNHEALTHY
import no.nav.paw.health.HealthCheck

const val startupPath = "/internal/hasStarted"

interface StartupCheck: HealthCheck {
    fun hasStarted(): Boolean
}

fun hasStarted(function: () -> Boolean) = object : StartupCheck {
    override fun hasStarted(): Boolean = function()
}

fun Route.startupRoute(vararg startupChecks: StartupCheck) {
    get(startupPath) {
        val startupComplete = startupChecks.all { startupCheck -> startupCheck.hasStarted() }
        when (startupComplete) {
            true -> call.respondText(contentType = Text.Plain, status = OK) { HEALTHY.value }
            false -> call.respondText(contentType = Text.Plain, status = ServiceUnavailable) { UNHEALTHY.value }
        }
    }
}
