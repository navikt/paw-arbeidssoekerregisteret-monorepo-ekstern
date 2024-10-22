package no.nav.paw.arbeidssoekerregisteret.api.oppslag.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.StatusException
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.AuthorizationService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.BekreftelseService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.PeriodeService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.asUUID
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.getPidClaim
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.verifyAccessFromToken
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.verifyPeriodeId

fun Route.bekreftelseRoutes(
    authorizationService: AuthorizationService,
    bekreftelseService: BekreftelseService,
    periodeService: PeriodeService
) {
    route("/api/v1") {
        authenticate("tokenx") {
            get("/arbeidssoekerbekreftelser/{periodeId}") {
                val identitetsnummer = call.getPidClaim()
                val periodeId = call.parameters["periodeId"]?.asUUID()
                    ?: throw StatusException(HttpStatusCode.BadRequest, "mangler periodeId")
                val identitetsnummerList = authorizationService.finnIdentiteter(identitetsnummer)

                verifyPeriodeId(periodeId, identitetsnummerList, periodeService)
                val response = bekreftelseService.finnBekreftelserForPeriodeId(periodeId)
                call.respond(response)
            }
        }

        authenticate("azure") {
            get("/veileder/arbeidssoekerbekreftelser/{periodeId}") {
                val periodeId = call.parameters["periodeId"]?.asUUID()
                    ?: throw StatusException(HttpStatusCode.BadRequest, "mangler periodeId")

                val periode = periodeService.hentPeriodeForId(periodeId) ?: throw StatusException(
                    HttpStatusCode.BadRequest,
                    "Finner ikke periode $periodeId"
                )
                val identitetsnummerList = authorizationService
                    .finnIdentiteter(Identitetsnummer(periode.identitetsnummer))
                call.verifyAccessFromToken(authorizationService, identitetsnummerList)

                val response = bekreftelseService.finnBekreftelserForPeriodeId(periodeId)
                call.respond(response)
            }
        }
    }
}