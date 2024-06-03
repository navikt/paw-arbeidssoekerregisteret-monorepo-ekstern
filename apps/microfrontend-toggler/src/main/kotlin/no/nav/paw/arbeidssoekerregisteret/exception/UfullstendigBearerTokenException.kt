package no.nav.paw.arbeidssoekerregisteret.exception

class UfullstendigBearerTokenException(
    override val message: String
) : AuthorizationException("UFULLSTENDING_BEARER_TOKEN", message)