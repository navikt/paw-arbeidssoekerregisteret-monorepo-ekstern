package no.nav.paw.arbeidssoekerregisteret.api.oppslag.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ArbeidssoekerperiodeRequest
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.AuthorizationService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.OpplysningerService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.PeriodeService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.ProfileringService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.buildApplicationLogger
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.createSamletInformasjonResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.createSisteSamletInformasjonResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.getPidClaim
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.getRequestBody
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.verifyAccessFromToken

private val logger = buildApplicationLogger

fun Route.samletInformasjonRoutes(
    authorizationService: AuthorizationService,
    periodeService: PeriodeService,
    opplysningerOmArbeidssoekerService: OpplysningerService,
    profileringService: ProfileringService
) {
    route("/api/v1") {

        authenticate("tokenx") {
            get("/samlet-informasjon") {
                val siste = call.request.queryParameters["siste"]?.toBoolean() ?: false
                val identitetsnummer = call.getPidClaim()
                val identitetsnummerList = authorizationService.finnIdentiteter(identitetsnummer)

                val response = if (siste) {
                    // TODO Fiks med order by og limit mot databasen
                    createSisteSamletInformasjonResponse(
                        identitetsnummerList,
                        periodeService,
                        opplysningerOmArbeidssoekerService,
                        profileringService
                    )
                } else {
                    createSamletInformasjonResponse(
                        identitetsnummerList,
                        periodeService,
                        opplysningerOmArbeidssoekerService,
                        profileringService
                    )
                }

                logger.info("Hentet siste samlet informasjon for bruker")

                call.respond(HttpStatusCode.OK, response)
            }
        }

        authenticate("azure") {
            post("/veileder/samlet-informasjon") {
                val siste = call.request.queryParameters["siste"]?.toBoolean() ?: false
                val (identitetsnummer) = call.getRequestBody<ArbeidssoekerperiodeRequest>()
                val identitetsnummerList = authorizationService.finnIdentiteter(Identitetsnummer(identitetsnummer))

                call.verifyAccessFromToken(authorizationService, identitetsnummerList)

                val response = if (siste) {
                    // TODO Fiks med order by og limit mot databasen
                    createSisteSamletInformasjonResponse(
                        identitetsnummerList,
                        periodeService,
                        opplysningerOmArbeidssoekerService,
                        profileringService
                    )
                } else {
                    createSamletInformasjonResponse(
                        identitetsnummerList,
                        periodeService,
                        opplysningerOmArbeidssoekerService,
                        profileringService
                    )
                }

                logger.info("Veileder hentet siste samlet informasjon for bruker")

                call.respond(HttpStatusCode.OK, response)
            }
        }
    }
}
