package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.route

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingRequest
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.opentelemetry.api.common.AttributeKey.booleanKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.model.EgenvurderingDialogRequest
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.model.EgenvurderingDialogResponse
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.repository.PeriodeDialogRow
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.service.DialogService
import no.nav.paw.error.model.ErrorType
import no.nav.paw.error.model.ProblemDetails
import no.nav.paw.error.model.ProblemDetailsBuilder
import no.nav.paw.security.authentication.model.AzureAd
import no.nav.paw.security.authentication.plugin.autentisering

fun Route.egenvurderingRoutes(
    dialogService: DialogService,
) {
    route("/api/v1/egenvurdering") {
        autentisering(AzureAd) {
            post<EgenvurderingDialogRequest>("/dialog") { request ->
                val dialogInfo: PeriodeDialogRow? = dialogService.finnDialogInfoForPeriodeId(request.periodeId)
                Span.current().addEvent(
                    "egenvurdering-route",
                    Attributes.of(
                        booleanKey("periodeId"), dialogInfo?.periodeId != null,
                        booleanKey("dialogId"), dialogInfo?.dialogId != null
                    ),
                )

                val (httpCode, responseObject) =  when {
                    dialogInfo == null -> HttpStatusCode.NotFound to notFoundProblemDetails(call.request)

                    dialogInfo.finnSisteAuditRow()?.dialogHttpStatusCode == HttpStatusCode.Conflict.value ->
                        HttpStatusCode.NoContent to null

                    dialogInfo.dialogId != null ->
                        HttpStatusCode.OK to EgenvurderingDialogResponse(dialogId = dialogInfo.dialogId)

                    else -> HttpStatusCode.InternalServerError to internalServerErrorProblemDetails(call.request)
                }

                when {
                    responseObject != null -> call.respond(httpCode, responseObject)
                    else -> call.respond(httpCode)
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

val EGENVURDERING_DIALOG_TJENESTE_INTERNAL_SERVER_ERROR = ErrorType.domain("egenvurdering").error("internal-server-error").build()
private fun internalServerErrorProblemDetails(request: RoutingRequest): ProblemDetails = ProblemDetailsBuilder.builder()
    .type(EGENVURDERING_DIALOG_TJENESTE_INTERNAL_SERVER_ERROR)
    .status(HttpStatusCode.InternalServerError)
    .detail("Noe gikk galt ved henting av dialog for arbeidssøkerperiode")
    .instance(request.uri)
    .build()