package no.nav.paw.health

import io.ktor.http.ContentType.Text
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.HttpStatusCode.Companion.ServiceUnavailable
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.paw.health.HealthStatus.HEALTHY
import no.nav.paw.health.HealthStatus.UNHEALTHY

const val readinessPath = "/internal/isReady"

interface ReadinessCheck: HealthCheck {
    fun isReady(): Boolean
}

fun isReady(function: () -> Boolean) = object : ReadinessCheck {
    override fun isReady(): Boolean = function()
}

fun Route.readinessRoute(vararg readinessChecks: ReadinessCheck) {
    get(readinessPath) {
        val applicationReady = readinessChecks.all { readinessCheck -> readinessCheck.isReady() }
        when (applicationReady) {
            true -> call.respondText(contentType = Text.Plain, status = OK) { HEALTHY.value }
            false -> call.respondText(contentType = Text.Plain, status = ServiceUnavailable) { UNHEALTHY.value }
        }
    }
}