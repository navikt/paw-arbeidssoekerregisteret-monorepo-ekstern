package no.nav.paw.rapportering.api.routes

import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.paw.rapportering.api.domain.request.RapporteringRequest

fun Route.rapporteringRoutes() {
    route("/api/v1") {
        authenticate("tokenx", "azure") {
            post<RapporteringRequest>("/rapportering") { rapportering ->
                // sjekke periodeId opp mot identitetsnummer
                // RapporteringTilgjengelig -> kan rapportere
                // RapporteringsMeldingMottatt eller PeriodeAvsluttet -> sletter rapporteringsmulighet

                // sende rapportering ut på rapporteringstopic
                call.respond("Rapportering mottatt")
            }
        }
    }
}