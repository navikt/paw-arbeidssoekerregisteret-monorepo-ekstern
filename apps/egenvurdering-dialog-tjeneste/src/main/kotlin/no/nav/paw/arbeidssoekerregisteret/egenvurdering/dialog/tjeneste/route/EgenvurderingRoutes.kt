package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.route

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingRequest
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.model.EgenvurderingDialogRequest
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.model.EgenvurderingDialogResponse
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.service.DialogService
import no.nav.paw.error.model.ErrorType
import no.nav.paw.error.model.ProblemDetails
import no.nav.paw.error.model.ProblemDetailsBuilder
import no.nav.paw.logging.logger.buildApplicationLogger
import no.nav.paw.security.authentication.model.AzureAd
import no.nav.paw.security.authentication.plugin.autentisering

val logger = buildApplicationLogger

fun Route.egenvurderingRoutes(
    dialogService: DialogService,
) {
    route("/api/v1/egenvurdering") {
        autentisering(AzureAd) {
            post<EgenvurderingDialogRequest>("/dialog") { request ->
                val dialogInfo = dialogService.finnDialogInfoForPeriodeId(request.periodeId)
                when {
                    dialogInfo == null -> {
                        logger.warn("Fant ikke dialog med for periode ${request.periodeId}")
                        call.respond(HttpStatusCode.NotFound, notFoundProblemDetails(call.request))
                    }

                    dialogInfo.finnSisteAuditRow()?.dialogHttpStatusCode == HttpStatusCode.Conflict.value ->
                        call.respond(HttpStatusCode.NoContent)

                    dialogInfo.dialogId != null ->
                        call.respond(EgenvurderingDialogResponse(dialogId = dialogInfo.dialogId))
                }
            }
        }
    }
}

val DIALOG_IKKE_FUNNET_ERROR_TYPE = ErrorType.domain("egenvurdering").error("dialog-ikke-funnet").build()

private fun notFoundProblemDetails(request: RoutingRequest): ProblemDetails = ProblemDetailsBuilder.builder()
    .type(DIALOG_IKKE_FUNNET_ERROR_TYPE)
    .status(HttpStatusCode.NotFound)
    .detail("Dialog ikke funnet for arbeidssøkerperiode")
    .instance(request.uri)
    .build()