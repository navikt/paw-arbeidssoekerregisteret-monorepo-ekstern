package no.nav.paw.oppslagapi.routes.v3

import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.paw.oppslagapi.data.query.ApplicationQueryLogic
import no.nav.paw.oppslagapi.mapping.v3.finnSistePeriode
import no.nav.paw.oppslagapi.model.v3.AggregertPeriode
import no.nav.paw.oppslagapi.model.v3.QueryRequest
import no.nav.paw.oppslagapi.model.v3.Tidslinje
import no.nav.paw.oppslagapi.plugin.installContentNegotiation
import no.nav.paw.oppslagapi.plugin.installErrorHandler
import no.nav.paw.oppslagapi.respondWith
import no.nav.paw.oppslagapi.utils.configureJacksonForV3
import no.nav.paw.security.authentication.model.AzureAd
import no.nav.paw.security.authentication.model.TokenX
import no.nav.paw.security.authentication.model.securityContext
import no.nav.paw.security.authentication.plugin.autentisering

fun Route.v3Routes(
    queryLogic: ApplicationQueryLogic
) {
    route("/api/v3") {
        installContentNegotiation {
            configureJacksonForV3()
        }
        installErrorHandler()

        autentisering(TokenX, AzureAd) {
            post<QueryRequest>("/snapshot") { request ->
                val securityContext = call.securityContext()
                val response = queryLogic.finnTidslinjer(
                    securityContext = securityContext,
                    request = request
                ).finnSistePeriode()
                call.respondWith<AggregertPeriode>(response)
            }

            post<QueryRequest>("/perioder") { request ->
                val securityContext = call.securityContext()
                val response = queryLogic.finnTidslinjer(
                    securityContext = securityContext,
                    request = request,
                    types = call.types(),
                    ordering = call.ordering()
                )
                call.respondWith<List<Tidslinje>>(response)
            }
        }
    }
}