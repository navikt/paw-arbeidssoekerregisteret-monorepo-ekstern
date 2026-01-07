package no.nav.paw.ledigestillinger.route

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.paw.ledigestillinger.exception.MalformedRequestException
import no.nav.paw.ledigestillinger.exception.RequestParamMissingException
import no.nav.paw.ledigestillinger.model.asResponse
import no.nav.paw.ledigestillinger.service.StillingService
import no.nav.paw.ledigestillinger.service.StillingServiceV2
import no.nav.paw.security.authentication.model.TokenX
import no.nav.paw.security.authentication.plugin.autentisering
import no.naw.paw.ledigestillinger.model.FinnStillingerByEgenskaperRequest
import no.naw.paw.ledigestillinger.model.FinnStillingerByUuidListeRequest
import no.naw.paw.ledigestillinger.model.FinnStillingerRequest
import no.naw.paw.ledigestillinger.model.FinnStillingerResponse
import no.naw.paw.ledigestillinger.model.PagingResponse
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
                    is FinnStillingerByUuidListeRequest -> {
                        val stillinger = stillingService.finnStillingerByUuidListe(request.uuidListe)
                        val response = FinnStillingerResponse(
                            stillinger = stillinger,
                            paging = PagingResponse(
                                page = 1,
                                pageSize = request.uuidListe.size,
                                hitSize = stillinger.size
                            )
                        )

                        call.respond<FinnStillingerResponse>(response)
                    }

                    is FinnStillingerByEgenskaperRequest -> {
                        request.verify()
                        val stillinger = stillingService.finnStillingerByEgenskaper(
                            medSoekeord = request.soekeord,
                            medStyrkkoder = request.styrkkoder,
                            medFylker = request.fylker,
                            paging = request.paging
                        )
                        val response = FinnStillingerResponse(
                            stillinger = stillinger,
                            paging = request.paging.asResponse(hitSize = stillinger.size)
                        )

                        call.respond<FinnStillingerResponse>(response)
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