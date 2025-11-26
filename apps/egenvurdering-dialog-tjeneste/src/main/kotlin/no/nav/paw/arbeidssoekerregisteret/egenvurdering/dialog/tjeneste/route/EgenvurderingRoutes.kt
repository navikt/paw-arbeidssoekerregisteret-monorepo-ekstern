package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.route

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
                val dialogId =
                    dialogService.finnDialogIdForPeriodeId(request.periodeId) ?: throw DialogIkkeFunnetException()
                val response = EgenvurderingDialogResponse(
                    dialogId = dialogId
                )
                call.respond<EgenvurderingDialogResponse>(response)
            }
        }
    }
}