package no.nav.paw.arbeidssoekerregisteret.eksternt.api.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.models.EksternRequest
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.models.getIdentitetsnummer
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.services.PeriodeService
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils.buildApplicationLogger
import java.time.LocalDate
import java.time.format.DateTimeParseException

private val logger = buildApplicationLogger

fun Route.periodeRoutes(periodeService: PeriodeService) {
    route("/api/v1/arbeidssoekerperioder") {
        authenticate("maskinporten") {
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
                            return@post call.respond(
                                HttpStatusCode.BadRequest,
                                "Ugyldig dato 'fraStartetDato' må være satt med yyyy-mm-dd"
                            )
                        }
                    }

                val identitetsnummer = eksternRequest.getIdentitetsnummer()

                val arbeidssoekerperioder = periodeService.hentPerioder(identitetsnummer, fraStartetDato)

                logger.info("Hentet arbeidssøkerperioder for bruker")

                return@post call.respond(HttpStatusCode.OK, arbeidssoekerperioder)
            }
        }
    }
}
