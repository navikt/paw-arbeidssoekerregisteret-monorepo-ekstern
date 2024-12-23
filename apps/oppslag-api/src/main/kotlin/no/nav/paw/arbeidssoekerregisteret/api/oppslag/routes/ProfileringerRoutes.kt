package no.nav.paw.arbeidssoekerregisteret.api.oppslag.routes

import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ProfileringRequest
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.AuthorizationService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.ProfileringService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.buildApplicationLogger
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.getPaging
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.periodeIdParam
import no.nav.paw.security.authentication.interceptor.autentisering
import no.nav.paw.security.authentication.model.AzureAd
import no.nav.paw.security.authentication.model.Identitetsnummer
import no.nav.paw.security.authentication.model.Sluttbruker
import no.nav.paw.security.authentication.model.TokenX
import no.nav.paw.security.authentication.model.bruker
import no.nav.paw.security.authorization.interceptor.autorisering
import no.nav.paw.security.authorization.model.Action

private val logger = buildApplicationLogger

fun Route.profileringRoutes(
    authorizationService: AuthorizationService,
    profileringService: ProfileringService
) {
    route("/api/v1/profilering") {
        autentisering(TokenX, authorizationService::utvidPrincipal) {
            get("/") {
                val paging = call.getPaging()
                val accessPolicies = authorizationService.sluttbrukerAccessPolicies()

                autorisering(Action.READ, accessPolicies) {
                    val sluttbruker = call.bruker<Sluttbruker>()

                    val response = profileringService
                        .finnProfileringerForIdentiteter(sluttbruker.alleIdenter, paging)
                    logger.info("Bruker hentet profilering")
                    call.respond(response)
                }
            }

            get("/{periodeId}") {
                val paging = call.getPaging()
                val periodeId = call.periodeIdParam()
                val accessPolicies = authorizationService.sluttbrukerAccessPolicies(periodeId)

                autorisering(Action.READ, accessPolicies) {
                    val response = profileringService.finnProfileringerForPeriodeIdList(listOf(periodeId), paging)
                    logger.info("Bruker hentet profilering")
                    call.respond(response)
                }
            }
        }
    }

    route("/api/v1/veileder/profilering") {
        autentisering(AzureAd) {
            post("/") {
                val paging = call.getPaging()
                val (identitetsnummer, periodeId) = call.receive<ProfileringRequest>()
                val identitetsnummerList = authorizationService.finnIdentiteter(Identitetsnummer(identitetsnummer))
                val accessPolicies = authorizationService.veilederAccessPolicies(periodeId, identitetsnummerList)

                autorisering(Action.READ, accessPolicies) {
                    val response = if (periodeId != null) {
                        profileringService.finnProfileringerForPeriodeIdList(listOf(periodeId), paging)
                    } else {
                        profileringService.finnProfileringerForIdentiteter(identitetsnummerList, paging)
                    }
                    logger.info("Veileder hentet profilering for bruker")
                    call.respond(response)
                }
            }
        }
    }
}
