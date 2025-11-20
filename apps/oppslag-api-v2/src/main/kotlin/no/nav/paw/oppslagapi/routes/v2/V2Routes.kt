package no.nav.paw.oppslagapi.routes.v2

import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.BekreftelserResponse
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.TidslinjeResponse
import no.nav.paw.error.model.map
import no.nav.paw.oppslagapi.data.query.ApplicationQueryLogic
import no.nav.paw.oppslagapi.model.v2.V2Request
import no.nav.paw.oppslagapi.model.v2.hentTidslinjer
import no.nav.paw.oppslagapi.plugin.installContentNegotiation
import no.nav.paw.oppslagapi.plugin.installErrorHandler
import no.nav.paw.oppslagapi.respondWith
import no.nav.paw.oppslagapi.utils.configureJacksonForV3
import no.nav.paw.security.authentication.model.AzureAd
import no.nav.paw.security.authentication.model.TokenX
import no.nav.paw.security.authentication.model.securityContext
import no.nav.paw.security.authentication.plugin.autentisering
import java.time.Instant

fun Route.v2Routes(
    queryLogic: ApplicationQueryLogic
) {
    route("/api/v2") {
        installContentNegotiation {
            configureJacksonForV3()
        }
        installErrorHandler()

        autentisering(TokenX, AzureAd) {
            post<V2Request>("/bekreftelser") { request ->
                val securityContext = call.securityContext()
                val response = queryLogic.hentTidslinjer(
                    securityContext = securityContext,
                    baseRequest = request.typedRequest
                ).map { tidslinjer ->
                    tidslinjer.flatMap { tidslinje ->
                        tidslinje.hendelser.mapNotNull { it.bekreftelseV1 }
                    }.sortedByDescending { it.bekreftelse?.svar?.sendtInnAv?.tidspunkt ?: Instant.EPOCH }
                }.map(::BekreftelserResponse)
                call.respondWith(response)
            }
            post<V2Request>("/tidslinjer") { request ->
                val securityContext = call.securityContext()
                val response = queryLogic.hentTidslinjer(
                    securityContext = securityContext,
                    baseRequest = request.typedRequest
                ).map(::TidslinjeResponse)
                call.respondWith(response)
            }
        }
    }
}