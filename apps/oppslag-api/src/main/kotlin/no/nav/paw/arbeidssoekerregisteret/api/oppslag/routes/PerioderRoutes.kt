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
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.PeriodeService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.buildApplicationLogger
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.getPidClaim
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.getRequestBody
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.verifyAccessFromToken

private val logger = buildApplicationLogger

fun Route.perioderRoutes(
    authorizationService: AuthorizationService, periodeService: PeriodeService
) {
    route("/api/v1") {

        authenticate("tokenx") {
            get("/arbeidssoekerperioder") {
                val siste = call.request.queryParameters["siste"]?.toBoolean() ?: false
                val identitetsnummer = call.getPidClaim()
                val identitetsnummerList = authorizationService.finnIdentiteter(identitetsnummer)

                val response = if (siste) {
                    val arbeidssoekerperioder = periodeService.finnPerioderForIdentiteter(identitetsnummerList)
                    // TODO Fiks med order by og limit mot databasen
                    arbeidssoekerperioder.maxByOrNull { it.startet.tidspunkt }?.let { listOf(it) } ?: emptyList()
                } else {
                    periodeService.finnPerioderForIdentiteter(identitetsnummerList)
                }

                logger.info("Hentet arbeidssøkerperioder for bruker")

                call.respond(HttpStatusCode.OK, response)
            }
        }

        authenticate("azure") {
            post("/veileder/arbeidssoekerperioder") {
                val siste = call.request.queryParameters["siste"]?.toBoolean() ?: false
                val (identitetsnummer) = call.getRequestBody<ArbeidssoekerperiodeRequest>()
                val identitetsnummerList = authorizationService.finnIdentiteter(Identitetsnummer(identitetsnummer))

                call.verifyAccessFromToken(authorizationService, identitetsnummerList)

                val response = if (siste) {
                    // TODO Fiks med order by og limit mot databasen
                    val arbeidssoekerperioder = periodeService.finnPerioderForIdentiteter(identitetsnummerList)
                    arbeidssoekerperioder.maxByOrNull { it.startet.tidspunkt }?.let { listOf(it) } ?: emptyList()
                } else {
                    periodeService.finnPerioderForIdentiteter(identitetsnummerList)
                }

                logger.info("Veileder hentet arbeidssøkerperioder for bruker")

                call.respond(HttpStatusCode.OK, response)
            }
        }
    }
}
