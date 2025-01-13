package no.nav.paw.security.authentication.model

import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authentication
import io.ktor.server.auth.principal
import no.nav.paw.security.authentication.token.AccessToken
import no.nav.paw.security.authentication.token.resolveTokens
import no.nav.paw.security.authorization.exception.SecurityContextManglerException
import no.nav.paw.security.authorization.exception.UgyldigBearerTokenException
import no.nav.paw.security.authorization.exception.UgyldigBrukerException
import no.nav.security.token.support.v3.TokenValidationContextPrincipal
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("no.nav.paw.logger.security.authentication")

data class SecurityContext(
    val bruker: Bruker<*>,
    val accessToken: AccessToken
)

@Suppress("LoggingSimilarMessage")
fun ApplicationCall.resolveSecurityContext(): SecurityContext {
    val principal = principal<TokenValidationContextPrincipal>()
    val tokenContext = principal?.context
        ?: throw UgyldigBearerTokenException("Ugyldig eller manglende Bearer Token")

    val accessToken = tokenContext.resolveTokens().firstOrNull() // Kan støtte flere tokens
        ?: throw UgyldigBearerTokenException("Ingen gyldige Bearer Tokens funnet")

    val bruker = when (accessToken.issuer) {
        is TokenX -> {
            logger.info("Autentiserer {} token -> {}", TokenX::class.simpleName, Sluttbruker::class.simpleName)
            Sluttbruker(accessToken.claims.getOrThrow(PID))
        }

        is AzureAd -> {
            if (accessToken.isM2MToken()) {
                val navIdentHeader = request.headers[NavIdentHeader.name]
                if (navIdentHeader.isNullOrBlank()) {
                    logger.info(
                        "Autentiserer {} M2M token -> {}",
                        AzureAd::class.simpleName,
                        Anonym::class.simpleName
                    )
                    Anonym(accessToken.claims.getOrThrow(OID))
                } else {
                    logger.info(
                        "Autentiserer {} M2M token -> {}",
                        AzureAd::class.simpleName,
                        NavAnsatt::class.simpleName
                    )
                    NavAnsatt(accessToken.claims.getOrThrow(OID), navIdentHeader)
                }
            } else {
                logger.info("Autentiserer {} token -> {}", AzureAd::class.simpleName, NavAnsatt::class.simpleName)
                NavAnsatt(accessToken.claims.getOrThrow(OID), accessToken.claims.getOrThrow(NavIdent))
            }
        }

        is IdPorten -> {
            logger.info("Autentiserer {} token -> {}", IdPorten::class.simpleName, Sluttbruker::class.simpleName)
            Sluttbruker(accessToken.claims.getOrThrow(PID))
        }

        is MaskinPorten -> {
            logger.info("Autentiserer {} token -> {}", MaskinPorten::class.simpleName, Anonym::class.simpleName)
            Anonym()
        }
    }

    return SecurityContext(
        bruker = bruker,
        accessToken = accessToken
    )
}

fun ApplicationCall.securityContext(): SecurityContext {
    return authentication.principal<SecurityContext>()
        ?: throw SecurityContextManglerException("Finner ikke security context principal")
}

fun ApplicationCall.securityContext(securityContext: SecurityContext) {
    authentication.principal(securityContext)
}

inline fun <reified T : Bruker<*>> SecurityContext.resolveBruker(): T {
    when (bruker) {
        is T -> return bruker
        else -> throw UgyldigBrukerException("Bruker er ikke av forventet type")
    }
}

inline fun <reified T : Bruker<*>> ApplicationCall.bruker(): T {
    return securityContext().resolveBruker()
}
