package no.nav.paw.arbeidssoekerregisteret.api.oppslag.routes

import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ArbeidssoekerperiodeRequest
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.AuthorizationService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.PeriodeService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.buildApplicationLogger
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.getPaging
import no.nav.paw.security.authentication.plugin.autentisering
import no.nav.paw.security.authentication.model.AzureAd
import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.security.authentication.model.Sluttbruker
import no.nav.paw.security.authentication.model.TokenX
import no.nav.paw.security.authentication.model.bruker
import no.nav.paw.security.authorization.interceptor.autorisering
import no.nav.paw.security.authorization.model.Action

private val logger = buildApplicationLogger

fun Route.perioderRoutes(
    authorizationService: AuthorizationService,
    periodeService: PeriodeService
) {
    route("/api/v1/arbeidssoekerperioder") {
        autentisering(TokenX, authorizationService::utvidPrincipal) {
            get("/") {
                val paging = call.getPaging()
                val accessPolicies = authorizationService.sluttbrukerAccessPolicies()

                autorisering(Action.READ, accessPolicies) {
                    val sluttbruker = call.bruker<Sluttbruker>()

                    val response = periodeService.finnPerioderForIdentiteter(sluttbruker.alleIdenter, paging)
                    logger.trace("Bruker hentet arbeidssøkerperioder")
                    call.respond(response)
                }
            }
        }
    }

    route("/api/v1/arbeidssoekerperioder-aggregert") {
        autentisering(TokenX, authorizationService::utvidPrincipal) {
            get("/") {
                val paging = call.getPaging()
                val accessPolicies = authorizationService.sluttbrukerAccessPolicies()

                autorisering(Action.READ, accessPolicies) {
                    val sluttbruker = call.bruker<Sluttbruker>()

                    val response = periodeService
                        .finnAggregertePerioderForIdenter(sluttbruker.alleIdenter, paging)
                    logger.trace("Bruker hentet aggregert arbeidssøkerperioder")
                    call.respond(response)
                }
            }
        }
    }

    route("/api/v1/veileder/arbeidssoekerperioder") {
        autentisering(AzureAd) {
            post("/") {
                val paging = call.getPaging()
                val (identitetsnummer) = call.receive<ArbeidssoekerperiodeRequest>()
                val identitetsnummerList = authorizationService.finnIdentiteter(Identitetsnummer(identitetsnummer))
                val accessPolicies = authorizationService.veilederAccessPolicies(identitetsnummerList)

                autorisering(Action.READ, accessPolicies) {
                    val response = periodeService.finnPerioderForIdentiteter(identitetsnummerList, paging)
                    logger.trace("Veileder hentet arbeidssøkerperioder for bruker")
                    call.respond(response)
                }
            }
        }
    }

    route("/api/v1/veileder/arbeidssoekerperioder-aggregert") {
        autentisering(AzureAd) {
            post("/") {
                val paging = call.getPaging()
                val (identitetsnummer) = call.receive<ArbeidssoekerperiodeRequest>()
                val identitetsnummerList = authorizationService.finnIdentiteter(Identitetsnummer(identitetsnummer))
                val accessPolicies = authorizationService.veilederAccessPolicies(identitetsnummerList)

                autorisering(Action.READ, accessPolicies) {
                    val response = periodeService.finnAggregertePerioderForIdenter(identitetsnummerList, paging)
                    logger.trace("Veileder hentet aggregert arbeidssøkerperioder for bruker")
                    call.respond(response)
                }
            }
        }
    }
}
