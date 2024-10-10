package no.nav.paw.arbeidssoekerregisteret.api.oppslag.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ProfileringRequest
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.AuthorizationService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.PeriodeService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.ProfileringService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.buildApplicationLogger
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.getPidClaim
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.getRequestBody
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.verifyAccessFromToken
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.verifyPeriodeId
import java.util.*

private val logger = buildApplicationLogger

fun Route.profileringRoutes(
    authorizationService: AuthorizationService,
    periodeService: PeriodeService,
    profileringService: ProfileringService
) {
    route("/api/v1") {

        authenticate("tokenx") {
            get("/profilering") {
                val siste = call.request.queryParameters["siste"]?.toBoolean() ?: false
                val identitetsnummer = call.getPidClaim()
                val identitetsnummerList = authorizationService.finnIdentiteter(identitetsnummer)

                val profileringer = profileringService
                    .finnProfileringerForIdentiteter(identitetsnummerList)

                val response = if (siste) {
                    profileringer.maxByOrNull { it.sendtInnAv.tidspunkt }?.let { listOf(it) } ?: emptyList()
                } else {
                    profileringer
                }

                logger.info("Hentet profilering for bruker")

                call.respond(HttpStatusCode.OK, response)
            }

            get("/profilering/{periodeId}") {
                val periodeId = UUID.fromString(call.parameters["periodeId"])
                val identitetsnummer = call.getPidClaim()
                val identitetsnummerList = authorizationService.finnIdentiteter(identitetsnummer)

                verifyPeriodeId(periodeId, identitetsnummerList, periodeService)

                val profileringer = profileringService.finnProfileringerForPeriodeId(periodeId)

                logger.info("Hentet profilering for bruker")

                call.respond(HttpStatusCode.OK, profileringer)
            }
        }

        authenticate("azure") {
            post("/veileder/profilering") {
                val siste = call.request.queryParameters["siste"]?.toBoolean() ?: false
                val (identitetsnummer, periodeId) = call.getRequestBody<ProfileringRequest>()
                val identitetsnummerList = authorizationService.finnIdentiteter(Identitetsnummer(identitetsnummer))

                call.verifyAccessFromToken(authorizationService, identitetsnummerList)

                val profileringer = if (periodeId != null) {
                    verifyPeriodeId(periodeId, identitetsnummerList, periodeService)

                    profileringService.finnProfileringerForPeriodeId(periodeId)
                } else {
                    profileringService.finnProfileringerForIdentiteter(identitetsnummerList)
                }

                val response = if (siste) {
                    // TODO Fiks med order by og limit mot databasen
                    profileringer.maxByOrNull { it.sendtInnAv.tidspunkt }?.let { listOf(it) } ?: emptyList()
                } else {
                    profileringer
                }

                logger.info("Veileder hentet profilering for bruker")

                call.respond(HttpStatusCode.OK, response)
            }
        }
    }
}
