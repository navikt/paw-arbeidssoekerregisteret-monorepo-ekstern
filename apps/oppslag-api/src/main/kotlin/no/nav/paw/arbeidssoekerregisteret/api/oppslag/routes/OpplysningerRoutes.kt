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
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.OpplysningerOmArbeidssoekerRequest
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Paging
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.AuthorizationService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.OpplysningerService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.PeriodeService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.buildApplicationLogger
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.getPidClaim
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.getRequestBody
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.verifyAccessFromToken
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.verifyPeriodeId
import java.util.*

private val logger = buildApplicationLogger

fun Route.opplysningerRoutes(
    authorizationService: AuthorizationService,
    periodeService: PeriodeService,
    opplysningerOmArbeidssoekerService: OpplysningerService,
) {
    route("/api/v1") {

        authenticate("tokenx") {
            get("/opplysninger-om-arbeidssoeker") {
                val siste = call.request.queryParameters["siste"]?.toBoolean() ?: false
                val identitetsnummer = call.getPidClaim()
                val identitetsnummerList = authorizationService.finnIdentiteter(identitetsnummer)

                val paging = if (siste) Paging(size = 1) else Paging()
                val response = opplysningerOmArbeidssoekerService
                    .finnOpplysningerForIdentiteter(identitetsnummerList, paging)

                logger.info("Hentet opplysninger for bruker")

                call.respond(HttpStatusCode.OK, response)
            }

            get("/opplysninger-om-arbeidssoeker/{periodeId}") {
                val periodeId = UUID.fromString(call.parameters["periodeId"])
                val identitetsnummer = call.getPidClaim()
                val identitetsnummerList = authorizationService.finnIdentiteter(identitetsnummer)

                verifyPeriodeId(periodeId, identitetsnummerList, periodeService)

                val opplysningerOmArbeidssoeker = opplysningerOmArbeidssoekerService
                    .finnOpplysningerForPeriodeId(periodeId)

                logger.info("Hentet opplysninger for bruker")

                call.respond(HttpStatusCode.OK, opplysningerOmArbeidssoeker)
            }
        }

        authenticate("azure") {
            post("/veileder/opplysninger-om-arbeidssoeker") {
                val siste = call.request.queryParameters["siste"]?.toBoolean() ?: false
                val (identitetsnummer, periodeId) = call.getRequestBody<OpplysningerOmArbeidssoekerRequest>()
                val identitetsnummerList = authorizationService.finnIdentiteter(Identitetsnummer(identitetsnummer))

                call.verifyAccessFromToken(authorizationService, identitetsnummerList)

                val paging = if (siste) Paging(size = 1) else Paging()
                val response = if (periodeId != null) {
                    verifyPeriodeId(periodeId, identitetsnummerList, periodeService)

                    opplysningerOmArbeidssoekerService.finnOpplysningerForPeriodeId(periodeId, paging)
                } else {
                    opplysningerOmArbeidssoekerService.finnOpplysningerForIdentiteter(identitetsnummerList, paging)
                }

                logger.info("Veileder hentet opplysninger-om-arbeidssøker for bruker")

                call.respond(HttpStatusCode.OK, response)
            }
        }
    }
}
