package no.nav.paw.oppslagapi.routes.v3

import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.paw.error.model.Data
import no.nav.paw.error.model.Response
import no.nav.paw.error.model.map
import no.nav.paw.logging.logger.buildApplicationLogger
import no.nav.paw.oppslagapi.data.query.ApplicationQueryLogic
import no.nav.paw.oppslagapi.data.query.PeriodeUtenStartHendelseException
import no.nav.paw.oppslagapi.mapping.v3.asV2Request
import no.nav.paw.oppslagapi.mapping.v3.asV3
import no.nav.paw.oppslagapi.mapping.v3.finnSistePeriode
import no.nav.paw.oppslagapi.model.v2.hentTidslinjer
import no.nav.paw.oppslagapi.model.v3.AggregertPeriode
import no.nav.paw.oppslagapi.model.v3.ListQueryRequest
import no.nav.paw.oppslagapi.model.v3.QueryRequest
import no.nav.paw.oppslagapi.model.v3.SingleQueryRequest
import no.nav.paw.oppslagapi.model.v3.Tidslinje
import no.nav.paw.oppslagapi.plugin.installContentNegotiation
import no.nav.paw.oppslagapi.plugin.installErrorHandler
import no.nav.paw.oppslagapi.respondWith
import no.nav.paw.oppslagapi.utils.configureJacksonForV3
import no.nav.paw.security.authentication.model.AzureAd
import no.nav.paw.security.authentication.model.SecurityContext
import no.nav.paw.security.authentication.model.TokenX
import no.nav.paw.security.authentication.model.securityContext
import no.nav.paw.security.authentication.plugin.autentisering

private val logger = buildApplicationLogger

fun Route.v3Routes(
    queryLogic: ApplicationQueryLogic
) {
    route("/api/v3") {
        installContentNegotiation {
            configureJacksonForV3()
        }
        installErrorHandler()

        autentisering(TokenX, AzureAd) {
            post<SingleQueryRequest>("/perioder") { request ->
                val securityContext = call.securityContext()
                val response = queryLogic.finnTidslinjer(
                    securityContext = securityContext,
                    request = request
                ).finnSistePeriode()
                call.respondWith<AggregertPeriode>(response)
            }

            post<ListQueryRequest>("/tidslinjer") { request ->
                val securityContext = call.securityContext()
                val response = queryLogic.finnTidslinjer(
                    securityContext = securityContext,
                    request = request
                )
                call.respondWith<List<Tidslinje>>(response)
            }
        }
    }
}

private suspend fun ApplicationQueryLogic.finnTidslinjer(
    securityContext: SecurityContext,
    request: QueryRequest
): Response<List<Tidslinje>> {
    return try {
        hentTidslinjer(
            securityContext = securityContext,
            baseRequest = request.asV2Request()
        ).map { tidslinjer ->
            tidslinjer.map { it.asV3() }
        }
    } catch (e: PeriodeUtenStartHendelseException) {
        logger.debug("Periode uten start-hendelse", e)
        Data(emptyList())
    }
}