package no.nav.paw.arbeidssoekerregisteret.eksternt.api.routes

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.models.EksternRequest
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.models.getFraStartetDato
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.models.getIdentitetsnummer
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.services.PeriodeService
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils.buildApplicationLogger
import no.nav.paw.security.authentication.interceptor.autentisering
import no.nav.paw.security.authentication.model.MaskinPorten

private val logger = buildApplicationLogger

fun Route.periodeRoutes(periodeService: PeriodeService) {
    route("/api/v1/arbeidssoekerperioder") {
        autentisering(MaskinPorten) {
            post<EksternRequest> { request ->
                logger.info("Henter arbeidssøkerperioder for bruker")

                val identitetsnummer = request.getIdentitetsnummer()
                val fraStartetDato = request.getFraStartetDato()

                val arbeidssoekerperioder = periodeService.hentPerioder(identitetsnummer, fraStartetDato)
                call.respond(arbeidssoekerperioder)
                logger.debug("Hentet arbeidssøkerperioder for bruker")
            }
        }
    }
}
