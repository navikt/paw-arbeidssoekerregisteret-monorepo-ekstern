package no.nav.paw.arbeidssoekerregisteret.api.oppslag.routes

import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ArbeidssoekerperiodeRequest
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.SamletInformasjonResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.AuthorizationService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.BekreftelseService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.OpplysningerService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.PeriodeService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.ProfileringService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.buildApplicationLogger
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.getPaging
import no.nav.paw.security.authentication.plugin.autentisering
import no.nav.paw.security.authentication.model.AzureAd
import no.nav.paw.model.Identitetsnummer
import no.nav.paw.security.authentication.model.Sluttbruker
import no.nav.paw.security.authentication.model.TokenX
import no.nav.paw.security.authentication.model.bruker
import no.nav.paw.security.authorization.interceptor.autorisering
import no.nav.paw.security.authorization.model.Action

private val logger = buildApplicationLogger

fun Route.samletInformasjonRoutes(
    authorizationService: AuthorizationService,
    periodeService: PeriodeService,
    opplysningerService: OpplysningerService,
    profileringService: ProfileringService,
    bekreftelseService: BekreftelseService
) {
    route("/api/v1/samlet-informasjon") {
        autentisering(TokenX, authorizationService::utvidPrincipal) {
            get("/") {
                val paging = call.getPaging()
                val accessPolicies = authorizationService.sluttbrukerAccessPolicies()

                autorisering(Action.READ, accessPolicies) {
                    val sluttbruker = call.bruker<Sluttbruker>()

                    val perioder = periodeService.finnPerioderForIdentiteter(sluttbruker.alleIdenter, paging)
                    val periodeIdList = perioder.map { it.periodeId }
                    val opplysninger = opplysningerService.finnOpplysningerForPeriodeIdList(periodeIdList, paging)
                    val profilering = profileringService.finnProfileringerForPeriodeIdList(periodeIdList, paging)
                    val bekreftelser = bekreftelseService.finnBekreftelserForPeriodeIdList(periodeIdList, paging)

                    val response = SamletInformasjonResponse(
                        arbeidssoekerperioder = perioder,
                        opplysningerOmArbeidssoeker = opplysninger,
                        profilering = profilering,
                        bekreftelser = bekreftelser
                    )

                    logger.trace("Bruker hentet samlet informasjon")

                    call.respond(response)
                }
            }
        }
    }

    route("/api/v1/veileder/samlet-informasjon") {
        autentisering(AzureAd) {
            post("/") {
                val paging = call.getPaging()
                val (identitetsnummer) = call.receive<ArbeidssoekerperiodeRequest>()
                val identitetsnummerList = authorizationService.finnIdentiteter(Identitetsnummer(identitetsnummer))
                val accessPolicies = authorizationService.veilederAccessPolicies(identitetsnummerList)

                autorisering(Action.READ, accessPolicies) {
                    val perioder = periodeService.finnPerioderForIdentiteter(identitetsnummerList, paging)
                    val periodeIdList = perioder.map { it.periodeId }
                    val opplysninger = opplysningerService.finnOpplysningerForPeriodeIdList(periodeIdList, paging)
                    val profilering = profileringService.finnProfileringerForPeriodeIdList(periodeIdList, paging)
                    val bekreftelser = bekreftelseService.finnBekreftelserForPeriodeIdList(periodeIdList, paging)

                    val response = SamletInformasjonResponse(
                        arbeidssoekerperioder = perioder,
                        opplysningerOmArbeidssoeker = opplysninger,
                        profilering = profilering,
                        bekreftelser = bekreftelser
                    )

                    logger.info("Veileder hentet siste samlet informasjon for bruker")
                    call.respond(response)
                }
            }
        }
    }
}
