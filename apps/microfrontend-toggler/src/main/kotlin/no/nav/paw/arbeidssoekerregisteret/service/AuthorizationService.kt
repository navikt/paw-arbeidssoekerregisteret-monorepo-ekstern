package no.nav.paw.arbeidssoekerregisteret.service

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssoekerregisteret.context.RequestContext
import no.nav.paw.arbeidssoekerregisteret.context.SecurityContext
import no.nav.paw.arbeidssoekerregisteret.exception.BearerTokenManglerException
import no.nav.paw.arbeidssoekerregisteret.exception.BrukerHarIkkeTilgangException
import no.nav.paw.arbeidssoekerregisteret.exception.UgyldigBearerTokenException
import no.nav.paw.arbeidssoekerregisteret.model.AccessToken
import no.nav.paw.arbeidssoekerregisteret.model.Azure
import no.nav.paw.arbeidssoekerregisteret.model.BrukerType
import no.nav.paw.arbeidssoekerregisteret.model.InnloggetBruker
import no.nav.paw.arbeidssoekerregisteret.model.PID
import no.nav.paw.arbeidssoekerregisteret.model.resolveTokens
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class AuthorizationService {
    private val logger: Logger = LoggerFactory.getLogger("no.nav.paw.logger.auth")

    @WithSpan
    suspend fun authorize(requestContext: RequestContext): SecurityContext {
        val tokenContext = requestContext.principal?.context ?: throw BearerTokenManglerException("Sesjon mangler")

        val accessToken = tokenContext.resolveTokens().firstOrNull()
            ?: throw UgyldigBearerTokenException("Ingen gyldige Bearer Tokens funnet")

        if (accessToken.claims.isEmpty()) {
            throw UgyldigBearerTokenException("Bearer Token mangler påkrevd innhold")
        }

        return SecurityContext(
            innloggetBruker = resolveInnloggetBruker(accessToken),
            accessToken = accessToken
        )
    }

    private fun resolveInnloggetBruker(accessToken: AccessToken): InnloggetBruker {
        return when (accessToken.issuer) {
            is Azure -> {
                throw BrukerHarIkkeTilgangException("Kun tilgang for sluttbruker")
            }

            else -> {
                val ident = accessToken.claims[PID].verdi
                InnloggetBruker(
                    type = BrukerType.SLUTTBRUKER,
                    ident = ident
                )
            }
        }
    }
}

