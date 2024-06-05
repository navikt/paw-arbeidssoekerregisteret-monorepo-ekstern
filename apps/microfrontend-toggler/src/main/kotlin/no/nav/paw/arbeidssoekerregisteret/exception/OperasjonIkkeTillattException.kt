package no.nav.paw.arbeidssoekerregisteret.exception

class OperasjonIkkeTillattException(
    override val message: String
) : AuthorizationException("OPERASJON_IKKE_TILLATT", message)