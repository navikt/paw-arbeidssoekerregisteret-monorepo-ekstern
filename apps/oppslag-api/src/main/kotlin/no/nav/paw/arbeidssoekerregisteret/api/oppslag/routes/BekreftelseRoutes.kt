package no.nav.paw.arbeidssoekerregisteret.api.oppslag.routes

import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.AuthorizationService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.BekreftelseService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.buildApplicationLogger
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.getPaging
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.periodeIdParam
import no.nav.paw.security.authentication.interceptor.autentisering
import no.nav.paw.security.authentication.model.AzureAd
import no.nav.paw.security.authentication.model.Sluttbruker
import no.nav.paw.security.authentication.model.TokenX
import no.nav.paw.security.authentication.model.bruker
import no.nav.paw.security.authorization.interceptor.autorisering
import no.nav.paw.security.authorization.model.Action

private val logger = buildApplicationLogger

fun Route.bekreftelseRoutes(
    authorizationService: AuthorizationService,
    bekreftelseService: BekreftelseService
) {
    route("/api/v1/arbeidssoekerbekreftelser") {
        autentisering(TokenX, authorizationService::utvidPrincipal) {
            get("/") {
                val paging = call.getPaging()
                val accessPolicies = authorizationService.sluttbrukerAccessPolicies()

                autorisering(Action.READ, accessPolicies) {
                    val sluttbruker = call.bruker<Sluttbruker>()

                    val response = bekreftelseService
                        .finnBekreftelserForIdentitetsnummerList(sluttbruker.alleIdenter, paging)
                    logger.info("Bruker hentet bekreftelser")
                    call.respond(response)
                }
            }

            get("/{periodeId}") {
                val paging = call.getPaging()
                val periodeId = call.periodeIdParam()
                val accessPolicies = authorizationService.sluttbrukerAccessPolicies(periodeId)

                autorisering(Action.READ, accessPolicies) {
                    val response = bekreftelseService.finnBekreftelserForPeriodeIdList(listOf(periodeId), paging)
                    logger.info("Bruker hentet bekreftelser for periode")
                    call.respond(response)
                }
            }
        }
    }

    route("/api/v1/veileder/arbeidssoekerbekreftelser") {
        autentisering(AzureAd) {
            get("/{periodeId}") {
                val paging = call.getPaging()
                val periodeId = call.periodeIdParam()
                val accessPolicies = authorizationService.veilederAccessPolicies(periodeId)

                autorisering(Action.READ, accessPolicies) {
                    val response = bekreftelseService.finnBekreftelserForPeriodeIdList(listOf(periodeId), paging)
                    logger.info("Veileder hentet bekreftelser for bruker")
                    call.respond(response)
                }
            }
        }
    }
}