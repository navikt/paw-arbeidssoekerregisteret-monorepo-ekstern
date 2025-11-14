package no.nav.paw.oppslagapi.routes.v3

import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.paw.error.model.map
import no.nav.paw.oppslagapi.data.query.ApplicationQueryLogic
import no.nav.paw.oppslagapi.hentTidslinjer
import no.nav.paw.oppslagapi.mapping.v3.asV2Request
import no.nav.paw.oppslagapi.mapping.v3.asV3
import no.nav.paw.oppslagapi.mapping.v3.finnSistePeriode
import no.nav.paw.oppslagapi.model.v3.AggregertPeriode
import no.nav.paw.oppslagapi.model.v3.ListQueryRequest
import no.nav.paw.oppslagapi.model.v3.SingleQueryRequest
import no.nav.paw.oppslagapi.model.v3.TidslinjeResponse
import no.nav.paw.oppslagapi.respondWith
import no.nav.paw.security.authentication.model.securityContext

fun Route.v3Routes(
    appQueryLogic: ApplicationQueryLogic
) {
    route("/api/v3") {
        post<SingleQueryRequest>("/perioder") { request ->
            val securityContext = call.securityContext()
            val response = appQueryLogic.hentTidslinjer(
                securityContext = securityContext,
                baseRequest = request.asV2Request()
            ).map { tidslinjer ->
                tidslinjer.map { it.asV3() }
            }.finnSistePeriode()
            call.respondWith<AggregertPeriode>(response)
        }
        post<ListQueryRequest>("/tidslinjer") { request ->
            val securityContext = call.securityContext()
            val response = appQueryLogic.hentTidslinjer(
                securityContext = securityContext,
                baseRequest = request.asV2Request()
            ).map { tidslinjer ->
                tidslinjer.map { it.asV3() }
            }.map(::TidslinjeResponse)
            call.respondWith<TidslinjeResponse>(response)
        }
    }
}