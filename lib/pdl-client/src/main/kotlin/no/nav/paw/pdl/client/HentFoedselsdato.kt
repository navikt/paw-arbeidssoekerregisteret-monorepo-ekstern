package no.nav.paw.pdl.client

import no.nav.paw.pdl.exception.PdlPersonIkkeFunnetException
import no.nav.paw.pdl.exception.PdlUkjentFeilException
import no.nav.paw.pdl.graphql.generated.HentFoedselsdato
import no.nav.paw.pdl.graphql.generated.hentfoedselsdato.Foedselsdato

suspend fun PdlClient.hentFoedselsdato(
    ident: String,
    callId: String?,
    traceparent: String? = null,
    navConsumerId: String?,
    behandlingsnummer: String,
): Foedselsdato? {
    val query = HentFoedselsdato(HentFoedselsdato.Variables(ident))

    logger.debug("Henter fødselsdato fra PDL")

    val response = execute(
        query = query,
        callId = callId,
        navConsumerId = navConsumerId,
        traceparent = traceparent,
        behandlingsnummer = behandlingsnummer,
    )

    response.errors?.let {
        logger.debug("Henter fødselsdato fra PDL feilet med: {}", it)
        if (response.hasNotFoundError()) {
            throw PdlPersonIkkeFunnetException("Person ikke funnet i PDL", it)
        } else {
            throw PdlUkjentFeilException("Kall til PDL feilet", it)
        }
    }

    logger.debug("Hentet fødselsdato fra PDL")

    return response
        .data
        ?.hentPerson
        ?.foedselsdato
        ?.firstOrNull()
}