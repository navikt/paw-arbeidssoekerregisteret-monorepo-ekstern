package no.nav.paw.oppslagapi.routes

import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.PerioderRequest
import no.nav.paw.oppslagapi.data.query.ApplicationQueryLogic
import no.nav.paw.oppslagapi.respondWith
import no.nav.paw.security.authentication.model.AzureAd
import no.nav.paw.security.authentication.model.TokenX
import no.nav.paw.security.authentication.model.securityContext
import no.nav.paw.security.authentication.plugin.autentisering

fun Route.v2Bekreftelse(appQueryLogic: ApplicationQueryLogic) {
    autentisering(TokenX, AzureAd) {
        post<PerioderRequest> { request ->
            val securityContext = call.securityContext()
            val response = appQueryLogic.hentBekreftelser(
                securityContext = securityContext,
                request = request
            )
            call.respondWith(response)
        }
    }
}
