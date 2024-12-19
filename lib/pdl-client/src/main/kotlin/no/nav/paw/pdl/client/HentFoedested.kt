package no.nav.paw.pdl.client

import no.nav.paw.pdl.exception.PdlPersonIkkeFunnetException
import no.nav.paw.pdl.exception.PdlUkjentFeilException
import no.nav.paw.pdl.graphql.generated.HentFoedested
import no.nav.paw.pdl.graphql.generated.hentfoedested.Foedested

suspend fun PdlClient.hentFoedested(
    ident: String,
    callId: String?,
    traceparent: String? = null,
    navConsumerId: String?,
    behandlingsnummer: String,
): Foedested? {
    val query = HentFoedested(HentFoedested.Variables(ident))

    logger.info("Henter fødested fra PDL")

    val response = execute(
        query = query,
        callId = callId,
        navConsumerId = navConsumerId,
        traceparent = traceparent,
        behandlingsnummer = behandlingsnummer,
    )

    response.errors?.let {
        logger.debug("Henter fødested fra PDL feilet med: {}", it)
        if (response.hasNotFoundError()) {
            throw PdlPersonIkkeFunnetException("Person ikke funnet i PDL", it)
        } else {
            throw PdlUkjentFeilException("Kall til PDL feilet", it)
        }
    }

    logger.debug("Hentet fødested fra PDL")

    return response
        .data
        ?.hentPerson
        ?.foedested
        ?.firstOrNull()
}