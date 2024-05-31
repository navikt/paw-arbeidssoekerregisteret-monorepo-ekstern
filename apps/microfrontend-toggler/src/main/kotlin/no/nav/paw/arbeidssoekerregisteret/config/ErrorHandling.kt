package no.nav.paw.arbeidssoekerregisteret.config

import com.fasterxml.jackson.databind.DatabindException
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.request.RequestAlreadyConsumedException
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import no.nav.paw.arbeidssoekerregisteret.context.LoggingContext
import no.nav.paw.arbeidssoekerregisteret.exception.ClientResponseException
import no.nav.paw.arbeidssoekerregisteret.exception.ServerResponseException
import no.nav.paw.arbeidssoekerregisteret.model.build400Error
import no.nav.paw.arbeidssoekerregisteret.model.build500Error
import no.nav.paw.arbeidssoekerregisteret.model.buildError

context(LoggingContext)
suspend fun <T : Throwable> handleException(call: ApplicationCall, throwable: T) {
    when (throwable) {
        is ContentTransformationException -> {
            val error = build400Error(
                "error:KUNNE_IKKE_TOLKE_INNHOLD",
                "Kunne ikke tolke innhold i kall",
                call.request.uri
            )
            logger.debug(error.detail, throwable)
            call.respond(error.status, error)
        }

        is DatabindException -> {
            val error = build400Error(
                "error:KUNNE_IKKE_TOLKE_JSON_INNHOLD",
                "Kunne ikke tolke JSON i kall",
                call.request.uri
            )
            logger.debug(error.detail, throwable)
            call.respond(error.status, error)
        }

        is ClientResponseException -> {
            val error = buildError(
                "error:${throwable.code}",
                throwable.message,
                throwable.status,
                call.request.uri
            )
            logger.warn(error.detail, throwable)
            call.respond(error.status, error)
        }

        is ServerResponseException -> {
            val error = buildError(
                "error:${throwable.code}",
                throwable.message,
                throwable.status,
                call.request.uri
            )
            logger.error(error.detail, throwable)
            call.respond(error.status, error)
        }

        is BadRequestException -> {
            val error =
                build400Error(
                    "error:ULOVLIG_FORESPOERSEL",
                    "Kunne ikke tolke innhold i forespørsel",
                    call.request.uri
                )
            logger.error(error.detail, throwable)
            call.respond(error.status, error)
        }

        is RequestAlreadyConsumedException -> {
            val error = build500Error(
                "error:FORESPOERSEL_ALLEREDE_MOTTATT",
                "Forespørsel er allerede mottatt. Dette er en kodefeil",
                call.request.uri
            )
            logger.error(error.detail, throwable)
            call.respond(error.status, error)
        }

        else -> {
            val error = build500Error(
                "error:UKJENT_FEIL",
                "Forespørsel feilet med ukjent feil",
                call.request.uri
            )
            logger.error(error.detail, throwable)
            call.respond(error.status, error)
        }
    }
}