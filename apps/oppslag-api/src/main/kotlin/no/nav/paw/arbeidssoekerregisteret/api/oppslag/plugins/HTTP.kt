package no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondText
import io.ktor.server.routing.IgnoreTrailingSlash
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.buildErrorLogger

private val logger = buildErrorLogger

fun Application.configureHTTP() {
    install(IgnoreTrailingSlash)
    install(StatusPages) {
        exception<StatusException> { call, cause ->
            logger.error("Request failed with status: ${cause.status}. Description: ${cause.description}")
            call.respondText(
                status = cause.status,
                text = cause.status.description
            )
        }
        exception<Throwable> { call, cause ->
            logger.info("Feil ved kall", cause)
            call.respondText(
                status = HttpStatusCode.InternalServerError,
                text = cause.message ?: HttpStatusCode.InternalServerError.description
            )
        }
    }
    install(CORS) {
        anyHost()

        allowMethod(io.ktor.http.HttpMethod.Options)
        allowMethod(io.ktor.http.HttpMethod.Put)
        allowMethod(io.ktor.http.HttpMethod.Patch)
        allowMethod(io.ktor.http.HttpMethod.Delete)

        allowHeader(io.ktor.http.HttpHeaders.Authorization)
        allowHeader(io.ktor.http.HttpHeaders.ContentType)
        allowHeader(io.ktor.http.HttpHeaders.AccessControlAllowOrigin)

        allowHeadersPrefixed("nav-")
    }
}
