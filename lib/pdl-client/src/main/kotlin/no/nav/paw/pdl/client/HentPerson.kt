package no.nav.paw.pdl.client

import no.nav.paw.pdl.exception.PdlPersonIkkeFunnetException
import no.nav.paw.pdl.exception.PdlUkjentFeilException
import no.nav.paw.pdl.graphql.generated.HentPerson
import no.nav.paw.pdl.graphql.generated.hentperson.Person

suspend fun PdlClient.hentPerson(
    ident: String,
    callId: String?,
    traceparent: String? = null,
    navConsumerId: String?,
    historisk: Boolean = false,
    behandlingsnummer: String,
): Person? {
    val query = HentPerson(HentPerson.Variables(ident, historisk))

    logger.trace("Henter person fra PDL")

    val response = execute(
        query = query,
        callId = callId,
        navConsumerId = navConsumerId,
        traceparent = traceparent,
        behandlingsnummer = behandlingsnummer,
    )

    response.errors?.let {
        logger.debug("Henter person fra PDL feilet med: {}", it)
        if (response.hasNotFoundError()) {
            throw PdlPersonIkkeFunnetException("Person ikke funnet i PDL", it)
        } else {
            throw PdlUkjentFeilException("Kall til PDL feilet", it)
        }
    }

    logger.trace("Hentet person fra PDL")

    return response
        .data
        ?.hentPerson
}
