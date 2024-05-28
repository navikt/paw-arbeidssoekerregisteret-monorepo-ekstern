package no.nav.paw.arbeidssoekerregisteret.config

import com.fasterxml.jackson.databind.DatabindException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.request.RequestAlreadyConsumedException
import io.ktor.server.response.respond
import no.nav.paw.arbeidssoekerregisteret.context.LoggingContext
import no.nav.paw.arbeidssoekerregisteret.exception.ClientResponseException
import no.nav.paw.arbeidssoekerregisteret.exception.ServerResponseException
import no.nav.paw.arbeidssoekerregisteret.model.ProblemDetails

context(LoggingContext)
suspend fun <T : Throwable> handleException(call: ApplicationCall, throwable: T) {
    when (throwable) {
        is ContentTransformationException -> {
            logger.debug("Bad request", throwable)
            val error = ProblemDetails("Bad request", HttpStatusCode.BadRequest, "Bad request", "")
            call.respond(error.status, error)
        }

        is DatabindException -> {
            logger.debug("Bad request", throwable)
            val error = ProblemDetails("Bad request", HttpStatusCode.BadRequest, "Bad request", "")
            call.respond(error.status, error)
        }

        is ClientResponseException -> {
            logger.warn("Request failed with status: ${throwable}. Description: ${throwable.message}")
            val error = ProblemDetails("Bad request", throwable.status, "Bad request", "")
            call.respond(error.status, error)
        }

        is ServerResponseException -> {
            logger.error("Request failed with status: ${throwable}. Description: ${throwable.message}")
            val error = ProblemDetails("Bad request", throwable.status, "Bad request", "")
            call.respond(error.status, error)
        }

        is BadRequestException -> {
            logger.error("Request failed: ${throwable.message}", throwable)
            val error = ProblemDetails("Bad request", HttpStatusCode.BadRequest, "Bad request", "")
            call.respond(error.status, error)
        }

        is RequestAlreadyConsumedException -> {
            logger.error("Request failed: {}", throwable.message, throwable)
            val error = ProblemDetails("Bad request", HttpStatusCode.InternalServerError, "Bad request", "")
            call.respond(error.status, error)
        }

        else -> {
            logger.error("Request failed with status: ${throwable}. Description: ${throwable.message}")
            val error = ProblemDetails("Unknown error", HttpStatusCode.InternalServerError, "Unknown error", "")
            call.respond(error.status, error)
        }
    }
}