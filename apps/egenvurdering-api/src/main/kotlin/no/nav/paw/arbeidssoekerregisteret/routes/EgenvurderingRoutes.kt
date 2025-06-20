package no.nav.paw.arbeidssoekerregisteret.routes

import io.ktor.http.HttpStatusCode
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.auth.parseAuthorizationHeader
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.api.models.EgenvurderingGrunnlag
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.api.models.EgenvurderingRequest
import no.nav.paw.arbeidssoekerregisteret.service.EgenvurderingService
import no.nav.paw.arbeidssoekerregisteret.utils.buildApplicationLogger
import no.nav.paw.client.api.oppslag.models.EgenvurderingResponse
import no.nav.paw.security.authentication.model.Sluttbruker
import no.nav.paw.security.authentication.model.TokenX
import no.nav.paw.security.authentication.model.bruker
import no.nav.paw.security.authentication.plugin.autentisering

val logger = buildApplicationLogger

private const val grunnlagPath = "/grunnlag"
const val egenvurderingPath = "/api/v1/arbeidssoeker/profilering/egenvurdering"
const val egenvurderingGrunnlagPath = egenvurderingPath + grunnlagPath
fun Route.egenvurderingRoutes(egenvurderingService: EgenvurderingService): Route {
    return route(egenvurderingPath) {
        autentisering(TokenX) {
            post<EgenvurderingRequest> { egenvurderingRequest ->
                logger.info("Mottok egenvurderingrequest")
                // TODO: Ta en runde på nødvendig innhold i EgenvurderingRequest
                call.respond(HttpStatusCode.Accepted)
            }
            get(grunnlagPath) {
                val bruker = call.bruker<Sluttbruker>()
                logger.info("Mottok grunnlagrequest")
                val userToken = call.request.jwt()
                val egenvurderingGrunnlag = egenvurderingService.getEgenvurderingGrunnlag(
                    identitetsnummer = bruker.ident,
                    userToken = userToken
                )
                call.respond(HttpStatusCode.OK, egenvurderingGrunnlag)
            }
        }
    }
}

internal fun ApplicationRequest.jwt(): String =
    this.parseAuthorizationHeader().let { authHeader ->
        (authHeader as? HttpAuthHeader.Single)?.blob ?: throw IllegalArgumentException("Fant ikke JWT i authorization header")
    }