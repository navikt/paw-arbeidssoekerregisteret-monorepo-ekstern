package no.nav.paw.arbeidssoekerregisteret.eksternt.api.routes

import io.ktor.http.*
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.models.EksternRequest
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.models.getIdentitetsnummer
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.services.ArbeidssoekerService
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils.logger
import java.time.LocalDate
import java.time.format.DateTimeParseException

fun Route.arbeidssoekerRoutes(arbeidssoekerService: ArbeidssoekerService) {
    route("/api/v1") {
        authenticate("maskinporten") {
            arbeidssoekerperiodeRoutes(arbeidssoekerService)
        }
    }
}

fun Route.arbeidssoekerperiodeRoutes(arbeidssoekerService: ArbeidssoekerService) {
    route("/arbeidssoekerperioder") {
        post {
            // Henter arbeidssøkerperiode for bruker
            logger.info("Henter arbeidssøkerperioder for bruker")

            val eksternRequest =
                try {
                    call.receive<EksternRequest>()
                } catch (e: BadRequestException) {
                    return@post call.respond(HttpStatusCode.BadRequest, "Ugyldig request body")
                }

            val fraStartetDato =
                eksternRequest.fraStartetDato?.let {
                    try {
                        LocalDate.parse(it)
                    } catch (e: DateTimeParseException) {
                        return@post call.respond(HttpStatusCode.BadRequest, "Ugyldig dato 'fraStartetDato' må være satt med yyyy-mm-dd")
                    }
                }

            val identitetsnummer = eksternRequest.getIdentitetsnummer()

            val arbeidssoekerperioder = arbeidssoekerService.hentArbeidssoekerperioder(identitetsnummer, fraStartetDato)

            logger.info("Hentet arbeidssøkerperioder for bruker")

            return@post call.respond(HttpStatusCode.OK, arbeidssoekerperioder)
        }
    }
}
