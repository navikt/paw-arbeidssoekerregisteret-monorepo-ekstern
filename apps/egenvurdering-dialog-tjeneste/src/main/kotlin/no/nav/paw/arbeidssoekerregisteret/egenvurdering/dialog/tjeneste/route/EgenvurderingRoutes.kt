package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.route

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.exception.DialogIkkeFunnetException
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.model.EgenvurderingDialogRequest
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.model.EgenvurderingDialogResponse
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.service.DialogService
import no.nav.paw.security.authentication.model.AzureAd
import no.nav.paw.security.authentication.plugin.autentisering

fun Route.egenvurderingRoutes(
    dialogService: DialogService
) {
    route("/api/v1/egenvurdering") {
        autentisering(AzureAd) {
            post<EgenvurderingDialogRequest>("/dialog") { request ->
                val dialogInfo = dialogService.finnDialogInfoForPeriodeId(request.periodeId)
                    ?: throw DialogIkkeFunnetException()
                when {
                    dialogInfo.dialogId != null ->
                        call.respond(EgenvurderingDialogResponse(dialogId = dialogInfo.dialogId))

                    dialogInfo.dialogHttpStatusCode == HttpStatusCode.Conflict.value ->
                        call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}