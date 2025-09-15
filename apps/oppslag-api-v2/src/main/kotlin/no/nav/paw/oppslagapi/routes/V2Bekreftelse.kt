package no.nav.paw.oppslagapi.routes

import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.BekreftelserResponse
import no.nav.paw.error.model.map
import no.nav.paw.oppslagapi.V2Request
import no.nav.paw.oppslagapi.data.query.ApplicationQueryLogic
import no.nav.paw.oppslagapi.hentTidslinjer
import no.nav.paw.oppslagapi.respondWith
import no.nav.paw.security.authentication.model.AzureAd
import no.nav.paw.security.authentication.model.TokenX
import no.nav.paw.security.authentication.model.securityContext
import no.nav.paw.security.authentication.plugin.autentisering
import java.time.Instant

fun Route.v2Bekreftelse(appQueryLogic: ApplicationQueryLogic) {
    autentisering(TokenX, AzureAd) {
        post<V2Request> { request ->
            val securityContext = call.securityContext()
            val response = appQueryLogic.hentTidslinjer(
                securityContext = securityContext,
                baseRequest = request.typedRequest
            ).map { tidslinjer ->
                tidslinjer.flatMap { tidslinje ->
                    tidslinje.hendelser.mapNotNull { it.bekreftelseV1 }
                }.sortedByDescending { it.bekreftelse?.svar?.sendtInnAv?.tidspunkt ?: Instant.EPOCH }
            }.map(::BekreftelserResponse)
            call.respondWith(response)
        }
    }
}
