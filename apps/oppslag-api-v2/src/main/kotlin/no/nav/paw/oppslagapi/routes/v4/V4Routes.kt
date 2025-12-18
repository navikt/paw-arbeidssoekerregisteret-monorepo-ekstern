package no.nav.paw.oppslagapi.routes.v4

import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.paw.oppslagapi.data.query.ApplicationQueryLogic
import no.nav.paw.oppslagapi.mapping.v3.asV2
import no.nav.paw.oppslagapi.mapping.v4.finnSistePeriodeV4
import no.nav.paw.oppslagapi.model.v3.QueryRequest
import no.nav.paw.oppslagapi.model.v4.AggregertPeriodeV4
import no.nav.paw.oppslagapi.model.v4.TidslinjeV4
import no.nav.paw.oppslagapi.plugin.installContentNegotiation
import no.nav.paw.oppslagapi.plugin.installErrorHandler
import no.nav.paw.oppslagapi.respondWith
import no.nav.paw.oppslagapi.routes.v3.ordering
import no.nav.paw.oppslagapi.routes.v3.types
import no.nav.paw.oppslagapi.utils.configureJacksonForV3
import no.nav.paw.security.authentication.model.AzureAd
import no.nav.paw.security.authentication.model.TokenX
import no.nav.paw.security.authentication.model.securityContext
import no.nav.paw.security.authentication.plugin.autentisering

fun Route.v4Routes(
    queryLogic: ApplicationQueryLogic
) {
    route("/api/v4") {
        installContentNegotiation {
            configureJacksonForV3()
        }
        installErrorHandler()

        autentisering(TokenX, AzureAd) {
            post<QueryRequest>("/snapshot") { request ->
                val securityContext = call.securityContext()
                val response = queryLogic.finnTidslinjerV4(
                    securityContext = securityContext,
                    request = request.asV2()
                ).finnSistePeriodeV4()
                call.respondWith<AggregertPeriodeV4>(response)
            }

            post<QueryRequest>("/perioder") { request ->
                val securityContext = call.securityContext()
                val response = queryLogic.finnTidslinjerV4(
                    securityContext = securityContext,
                    request = request.asV2(),
                    types = call.types(),
                    ordering = call.ordering()
                )
                call.respondWith<List<TidslinjeV4>>(response)
            }
        }
    }
}
