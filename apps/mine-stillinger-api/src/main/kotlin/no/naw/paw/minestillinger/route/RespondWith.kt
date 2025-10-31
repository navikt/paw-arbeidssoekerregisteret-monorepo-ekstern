package no.naw.paw.minestillinger.route

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingCall
import no.nav.paw.error.model.Data
import no.nav.paw.error.model.ProblemDetails
import no.nav.paw.error.model.Response

suspend fun RoutingCall.respondWith(response: Response<*>) {
    when (response) {
        is Data -> when (val data = response.data) {
            is Unit -> respond(HttpStatusCode.NoContent)
            null -> respond(HttpStatusCode.NoContent)
            else -> respond(HttpStatusCode.OK, data)
        }

        is ProblemDetails -> respond(response.status, response)
    }
}