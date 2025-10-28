package no.nav.paw.ledigestillinger.route

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.paw.error.exception.ServerResponseException
import no.nav.paw.error.model.ErrorType
import no.nav.paw.ledigestillinger.exception.MalformedRequestException
import no.nav.paw.ledigestillinger.exception.RequestParamMissingException
import no.nav.paw.ledigestillinger.model.asResponse
import no.nav.paw.ledigestillinger.service.StillingService
import no.nav.paw.security.authentication.model.TokenX
import no.nav.paw.security.authentication.plugin.autentisering
import no.naw.paw.ledigestillinger.model.FinnStillingerByEgenskaperRequest
import no.naw.paw.ledigestillinger.model.FinnStillingerByUuidListeRequest
import no.naw.paw.ledigestillinger.model.FinnStillingerRequest
import no.naw.paw.ledigestillinger.model.FinnStillingerResponse
import no.naw.paw.ledigestillinger.model.Stilling
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

            post<FinnStillingerRequest> { request ->
                when (request) {
                    is FinnStillingerByEgenskaperRequest -> {
                        request.verify()
                        val stillinger = stillingService.finnStillinger(
                            soekeord = request.soekeord,
                            kategorier = request.kategorier,
                            fylker = request.fylker,
                            paging = request.paging
                        )
                        val response = FinnStillingerResponse(
                            stillinger = stillinger,
                            paging = request.paging.asResponse(hitSize = stillinger.size)
                        )

                        call.respond<FinnStillingerResponse>(response)
                    }

                    is FinnStillingerByUuidListeRequest -> {
                        throw ServerResponseException(
                            status = HttpStatusCode.NotImplemented,
                            type = ErrorType.domain("stillinger").error("not-implemented").build(),
                            message = "Not yet implemented"
                        )
                    }
                }
            }
        }
    }
}

private fun FinnStillingerByEgenskaperRequest.verify() {
    if (paging.page < 1) throw MalformedRequestException("Page må være et positivt tall")
    if (paging.pageSize < 1) throw MalformedRequestException("Page size må være et positivt tall")
}