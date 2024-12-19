package no.nav.paw.pdl.client

import no.nav.paw.pdl.exception.PdlPersonIkkeFunnetException
import no.nav.paw.pdl.exception.PdlUkjentFeilException
import no.nav.paw.pdl.graphql.generated.HentForenkletStatus
import no.nav.paw.pdl.graphql.generated.hentforenkletstatus.Person

suspend fun PdlClient.hentForenkletStatus(
    ident: String,
    callId: String?,
    traceparent: String? = null,
    navConsumerId: String?,
    behandlingsnummer: String,
): Person? {
    val query = HentForenkletStatus(HentForenkletStatus.Variables(ident))

    logger.info("Henter forenklet status fra PDL")

    val response = execute(
        query = query,
        callId = callId,
        navConsumerId = navConsumerId,
        traceparent = traceparent,
        behandlingsnummer = behandlingsnummer,
    )

    response.errors?.let {
        logger.debug("Henter forenklet status fra PDL feilet med: {}", it)
        if (response.hasNotFoundError()) {
            throw PdlPersonIkkeFunnetException("Person ikke funnet i PDL", it)
        } else {
            throw PdlUkjentFeilException("Kall til PDL feilet", it)
        }
    }

    logger.debug("Hentet forenklet status fra PDL")

    return response
        .data
        ?.hentPerson
}
