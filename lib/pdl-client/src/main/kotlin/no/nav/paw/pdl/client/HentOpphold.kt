package no.nav.paw.pdl.client

import no.nav.paw.pdl.exception.PdlPersonIkkeFunnetException
import no.nav.paw.pdl.exception.PdlUkjentFeilException
import no.nav.paw.pdl.graphql.generated.HentOpphold
import no.nav.paw.pdl.graphql.generated.hentopphold.Opphold

suspend fun PdlClient.hentOpphold(
    ident: String,
    callId: String?,
    navConsumerId: String?,
    behandlingsnummer: String,
): List<Opphold>? {
    val query = HentOpphold(HentOpphold.Variables(ident))

    logger.info("Henter opphold fra PDL")

    val response = execute(
        query = query,
        callId = callId,
        navConsumerId = navConsumerId,
        behandlingsnummer = behandlingsnummer,
    )

    response.errors?.let {
        logger.debug("Henter opphold fra PDL feilet med: {}", it)
        if (response.hasNotFoundError()) {
            throw PdlPersonIkkeFunnetException("Person ikke funnet i PDL", it)
        } else {
            throw PdlUkjentFeilException("Kall til PDL feilet", it)
        }
    }

    logger.debug("Hentet opphold fra PDL")

    return response
        .data
        ?.hentPerson
        ?.opphold
}
