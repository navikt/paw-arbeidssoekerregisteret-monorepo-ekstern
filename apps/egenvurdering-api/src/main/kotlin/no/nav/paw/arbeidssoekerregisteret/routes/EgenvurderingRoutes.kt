package no.nav.paw.arbeidssoekerregisteret.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.api.models.EgenvurderingGrunnlag
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.api.models.EgenvurderingRequest
import no.nav.paw.arbeidssoekerregisteret.service.AuthorizationService
import no.nav.paw.arbeidssoekerregisteret.service.EgenvurderingService
import no.nav.paw.arbeidssoekerregisteret.utils.buildApplicationLogger
import no.nav.paw.security.authentication.model.ACR
import no.nav.paw.security.authentication.model.Sluttbruker
import no.nav.paw.security.authentication.model.TokenX
import no.nav.paw.security.authentication.model.bruker
import no.nav.paw.security.authentication.model.securityContext
import no.nav.paw.security.authentication.plugin.autentisering
import no.nav.paw.security.authorization.interceptor.autorisering
import no.nav.paw.security.authorization.model.Action

val logger = buildApplicationLogger

private const val grunnlagPath = "/grunnlag"
const val egenvurderingPath = "/api/v1/arbeidssoeker/profilering/egenvurdering"
const val egenvurderingGrunnlagPath = egenvurderingPath + grunnlagPath

fun Route.egenvurderingRoutes(
    authorizationService: AuthorizationService,
    egenvurderingService: EgenvurderingService,
): Route {
    return route(egenvurderingPath) {
        autentisering(TokenX) {

            get(grunnlagPath) {
                val accessPolicies = authorizationService.accessPolicies()
                autorisering(Action.READ, accessPolicies) {
                    val userToken = call.securityContext().accessToken.jwt
                    val egenvurderingGrunnlag = egenvurderingService.getEgenvurderingGrunnlag(userToken)
                    //val egenvurderingGrunnlag = EgenvurderingGrunnlag(grunnlag = null)
                    call.respond(HttpStatusCode.OK, egenvurderingGrunnlag)
                }
            }

            post<EgenvurderingRequest> { egenvurderingRequest ->
                val accessPolicies = authorizationService.accessPolicies()
                autorisering(Action.WRITE, accessPolicies) {
                    val sluttbruker = call.bruker<Sluttbruker>()
                    val accessToken = call.securityContext().accessToken
                    egenvurderingService.postEgenvurdering(sluttbruker.ident, egenvurderingRequest, accessToken)
                    call.respond(HttpStatusCode.Accepted)
                }
            }

        }
    }
}
