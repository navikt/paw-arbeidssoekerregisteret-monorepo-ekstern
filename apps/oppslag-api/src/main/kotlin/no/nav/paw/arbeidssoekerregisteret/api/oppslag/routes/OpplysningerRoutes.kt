package no.nav.paw.arbeidssoekerregisteret.api.oppslag.routes

import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.OpplysningerOmArbeidssoekerRequest
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.AuthorizationService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.OpplysningerService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.buildApplicationLogger
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.getPaging
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.periodeIdParam
import no.nav.paw.security.authentication.interceptor.autentisering
import no.nav.paw.security.authentication.model.AzureAd
import no.nav.paw.model.Identitetsnummer
import no.nav.paw.security.authentication.model.Sluttbruker
import no.nav.paw.security.authentication.model.TokenX
import no.nav.paw.security.authentication.model.bruker
import no.nav.paw.security.authorization.interceptor.autorisering
import no.nav.paw.security.authorization.model.Action

private val logger = buildApplicationLogger

fun Route.opplysningerRoutes(
    authorizationService: AuthorizationService,
    opplysningerOmArbeidssoekerService: OpplysningerService,
) {
    route("/api/v1/opplysninger-om-arbeidssoeker") {
        autentisering(TokenX, authorizationService::utvidPrincipal) {
            get("/") {
                val paging = call.getPaging()
                val accessPolicies = authorizationService.sluttbrukerAccessPolicies()

                autorisering(Action.READ, accessPolicies) {
                    val sluttbruker = call.bruker<Sluttbruker>()

                    val response = opplysningerOmArbeidssoekerService
                        .finnOpplysningerForIdentiteter(sluttbruker.alleIdenter, paging)
                    logger.info("Bruker hentet opplysninger")
                    call.respond(response)
                }
            }

            get("/{periodeId}") {
                val paging = call.getPaging()
                val periodeId = call.periodeIdParam()
                val accessPolicies = authorizationService.sluttbrukerAccessPolicies(periodeId)

                autorisering(Action.READ, accessPolicies) {
                    val response = opplysningerOmArbeidssoekerService
                        .finnOpplysningerForPeriodeIdList(listOf(periodeId), paging)
                    logger.info("Bruker hentet opplysninger")
                    call.respond(response)
                }
            }
        }
    }

    route("/api/v1/veileder/opplysninger-om-arbeidssoeker") {
        autentisering(AzureAd) {
            post("/") {
                val paging = call.getPaging()
                val (identitetsnummer, periodeId) = call.receive<OpplysningerOmArbeidssoekerRequest>()
                val identitetsnummerList = authorizationService.finnIdentiteter(Identitetsnummer(identitetsnummer))
                val accessPolicies = authorizationService.veilederAccessPolicies(periodeId, identitetsnummerList)

                autorisering(Action.READ, accessPolicies) {
                    val response = if (periodeId != null) {
                        opplysningerOmArbeidssoekerService.finnOpplysningerForPeriodeIdList(listOf(periodeId), paging)
                    } else {
                        opplysningerOmArbeidssoekerService.finnOpplysningerForIdentiteter(identitetsnummerList, paging)
                    }
                    logger.info("Veileder hentet opplysninger for bruker")
                    call.respond(response)
                }
            }
        }
    }
}
