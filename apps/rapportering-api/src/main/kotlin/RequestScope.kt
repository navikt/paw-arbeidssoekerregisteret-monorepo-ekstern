import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.principal
import io.ktor.util.pipeline.PipelineContext
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.rapportering.api.utils.AzureName
import no.nav.paw.rapportering.api.utils.AzureNavIdent
import no.nav.paw.rapportering.api.utils.AzureOID
import no.nav.paw.rapportering.api.utils.ResolvedClaims
import no.nav.paw.rapportering.api.utils.TokenXPID
import no.nav.paw.rapportering.api.utils.resolveClaims
import no.nav.security.token.support.v2.TokenValidationContextPrincipal

data class RequestScope(
    val claims: ResolvedClaims,
    val callId: String?,
    val traceparent: String?,
    val navConsumerId: String?,
)

context(PipelineContext<Unit, ApplicationCall>)
@WithSpan
fun requestScope(): RequestScope {
    val tokenValidationContext = call.principal<TokenValidationContextPrincipal>()
    val resolvedClaims = tokenValidationContext
        ?.context
        ?.resolveClaims(
            AzureName,
            AzureNavIdent,
            AzureOID,
            TokenXPID
        ) ?: ResolvedClaims()
    val headers = call.request.headers
    return RequestScope(
        claims = resolvedClaims,
        callId = headers["Nav-Call-Id"],
        traceparent = headers["traceparent"],
        navConsumerId = headers["Nav-Consumer-Id"]
    )
}