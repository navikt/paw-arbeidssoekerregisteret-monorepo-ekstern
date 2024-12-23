package no.nav.paw.pdl.client

import no.nav.paw.pdl.exception.PdlPersonIkkeFunnetException
import no.nav.paw.pdl.exception.PdlUkjentFeilException
import no.nav.paw.pdl.graphql.generated.HentForenkletStatusBolk
import no.nav.paw.pdl.graphql.generated.hentforenkletstatusbolk.HentPersonBolkResult

suspend fun PdlClient.hentForenkletStatusBolk(
    ident: List<String>,
    callId: String?,
    traceparent: String? = null,
    navConsumerId: String?,
    behandlingsnummer: String,
): List<HentPersonBolkResult>? {
    val query = HentForenkletStatusBolk(HentForenkletStatusBolk.Variables(ident))

    logger.debug("Henter forenklet status bolk fra PDL")

    val response = execute(
        query = query,
        callId = callId,
        navConsumerId = navConsumerId,
        traceparent = traceparent,
        behandlingsnummer = behandlingsnummer,
    )

    response.errors?.let {
        logger.debug("Henter forenklet status bolk fra PDL feilet med: {}", it)
        if (response.hasNotFoundError()) {
            throw PdlPersonIkkeFunnetException("Person ikke funnet i PDL", it)
        } else {
            throw PdlUkjentFeilException("Kall til PDL feilet", it)
        }
    }

    logger.debug("Hentet forenklet status bolk fra PDL")

    return response.data?.hentPersonBolk
}
