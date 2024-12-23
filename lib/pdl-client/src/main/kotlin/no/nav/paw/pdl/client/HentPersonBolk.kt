package no.nav.paw.pdl.client

import no.nav.paw.pdl.exception.PdlPersonIkkeFunnetException
import no.nav.paw.pdl.exception.PdlUkjentFeilException
import no.nav.paw.pdl.graphql.generated.HentPersonBolk
import no.nav.paw.pdl.graphql.generated.hentpersonbolk.HentPersonBolkResult

suspend fun PdlClient.hentPersonBolk(
    ident: List<String>,
    callId: String?,
    traceparent: String? = null,
    navConsumerId: String?,
    historisk: Boolean = false,
    behandlingsnummer: String,
): List<HentPersonBolkResult>? {
    val query = HentPersonBolk(HentPersonBolk.Variables(ident, historisk))

    logger.debug("Henter person bolk fra PDL")

    val response = execute(
        query = query,
        callId = callId,
        navConsumerId = navConsumerId,
        traceparent = traceparent,
        behandlingsnummer = behandlingsnummer,
    )

    response.errors?.let {
        logger.debug("Henter person bolk fra PDL feilet med: {}", it)
        if (response.hasNotFoundError()) {
            throw PdlPersonIkkeFunnetException("Person ikke funnet i PDL", it)
        } else {
            throw PdlUkjentFeilException("Kall til PDL feilet", it)
        }
    }

    logger.debug("Hentet person bolk fra PDL")

    return response
        .data
        ?.hentPersonBolk
}
