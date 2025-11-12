package no.nav.paw.arbeidssoekerregisteret

import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.api.models.EgenvurderingRequest
import no.nav.paw.arbeidssoekerregisteret.policy.SluttbrukerAccessPolicy
import no.nav.paw.arbeidssoekerregisteret.utils.buildApplicationLogger
import no.nav.paw.security.authentication.model.SecurityContext
import no.nav.paw.security.authentication.model.Sluttbruker
import no.nav.paw.security.authentication.model.TokenX
import no.nav.paw.security.authentication.model.securityContext
import no.nav.paw.security.authentication.plugin.autentisering
import no.nav.paw.security.authorization.interceptor.autorisering
import no.nav.paw.security.authorization.model.Action
import no.nav.paw.security.authorization.policy.AccessPolicy

val logger = buildApplicationLogger

private const val grunnlagPath = "/grunnlag"
const val egenvurderingPath = "/api/v1/arbeidssoeker/profilering/egenvurdering"
const val egenvurderingGrunnlagPath = egenvurderingPath + grunnlagPath

fun Route.egenvurderingRoutes(
    egenvurderingService: EgenvurderingService,
    accessPolicies: List<AccessPolicy> = listOf(SluttbrukerAccessPolicy()),
): Route {
    return route(egenvurderingPath) {
        autentisering(TokenX) {
            get(grunnlagPath) {
                autorisering(Action.READ, accessPolicies) {
                    val ident = call.securityContext().hentSluttbrukerEllerNull()?.ident
                        ?: throw BadRequestException("Kun støtte for tokenX (sluttbrukere)")
                    val egenvurderingGrunnlag = egenvurderingService.getEgenvurderingGrunnlag(ident)
                    call.respond(HttpStatusCode.OK, egenvurderingGrunnlag)
                }
            }

            post<EgenvurderingRequest> { egenvurderingRequest ->
                autorisering(Action.WRITE, accessPolicies) {
                    egenvurderingService.publiserOgLagreEgenvurdering(egenvurderingRequest, call.securityContext())
                    call.respond(HttpStatusCode.Accepted)
                }
            }

        }
    }
}

fun SecurityContext.hentSluttbrukerEllerNull(): Sluttbruker? = (this.bruker as? Sluttbruker)
