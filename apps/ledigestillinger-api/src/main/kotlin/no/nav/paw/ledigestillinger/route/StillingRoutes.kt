package no.nav.paw.ledigestillinger.route

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.paw.ledigestillinger.api.models.FinnStillingerRequest
import no.nav.paw.ledigestillinger.api.models.FinnStillingerResponse
import no.nav.paw.ledigestillinger.api.models.Stilling
import no.nav.paw.ledigestillinger.exception.RequestParamMissingException
import no.nav.paw.ledigestillinger.service.StillingService
import no.nav.paw.security.authentication.model.TokenX
import no.nav.paw.security.authentication.plugin.autentisering
import java.util.*

fun Route.stillingRoutes(
    stillingService: StillingService
) {
    route("/api/v1/stillinger") {
        autentisering(TokenX) {
            get("/{uuid}") {
                val uuid = call.parameters["uuid"]?.let { UUID.fromString(it) }
                    ?: throw RequestParamMissingException("Mangler uuid")
                val stilling = stillingService.hentStilling(uuid)
                call.respond<Stilling>(stilling)
            }
            post<FinnStillingerRequest>("/") { request ->
                val stillinger = stillingService.finnStillinger(
                    soekeord = request.soekeord ?: emptyList(),
                    kategorier = request.kategorier,
                    fylker = request.fylker
                )
                call.respond<FinnStillingerResponse>(FinnStillingerResponse(stillinger))
            }
        }
    }
}