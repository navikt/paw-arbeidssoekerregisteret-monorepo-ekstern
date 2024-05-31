package no.nav.paw.arbeidssoekerregisteret.routes.auth

import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authentication
import io.ktor.util.pipeline.PipelineContext
import no.nav.paw.arbeidssoekerregisteret.model.Claim
import no.nav.paw.arbeidssoekerregisteret.model.ResolvedClaim
import no.nav.paw.arbeidssoekerregisteret.model.ResolvedClaims
import no.nav.paw.arbeidssoekerregisteret.model.claimsList
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.v2.TokenValidationContextPrincipal

context(PipelineContext<Unit, ApplicationCall>)
fun ApplicationCall.resolveClaims(provider: String? = null): ResolvedClaims? {
    return this.authentication.principal<TokenValidationContextPrincipal>(provider)
        ?.context
        ?.getResolvedClaims()
}

fun TokenValidationContext?.resolveClaim(issuer: String, claimName: String): String? =
    this?.getClaims(issuer)?.getStringClaim(claimName)

fun TokenValidationContext?.getResolvedClaims(): ResolvedClaims = claimsList.mapNotNull { (issuer, claimName) ->
    this.resolveClaim(issuer, claimName)?.let { claimValue ->
        ResolvedClaim(issuer, Claim(claimName, claimValue))
    }
}
