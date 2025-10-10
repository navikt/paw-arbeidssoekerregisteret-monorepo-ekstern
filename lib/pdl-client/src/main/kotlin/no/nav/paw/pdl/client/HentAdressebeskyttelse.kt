package no.nav.paw.pdl.client

import no.nav.paw.pdl.exception.PdlPersonIkkeFunnetException
import no.nav.paw.pdl.exception.PdlUkjentFeilException
import no.nav.paw.pdl.graphql.generated.HentAdressebeskyttelse
import no.nav.paw.pdl.graphql.generated.hentadressebeskyttelse.Adressebeskyttelse

suspend fun PdlClient.hentAdressebeskyttelse(
    ident: String,
    navConsumerId: String?,
    behandlingsnummer: String,
): List<Adressebeskyttelse>? {
    val query = HentAdressebeskyttelse(HentAdressebeskyttelse.Variables(ident = ident))
    logger.debug("Henter adressebeskyttelse fra PDL")
    val response = execute(
        query = query,
        callId = null,
        navConsumerId = navConsumerId,
        behandlingsnummer = behandlingsnummer,
    )
    response.errors?.let {
        logger.debug("Hent adressebeskyttelse fra PDL feilet med: {}", it)
        if (response.hasNotFoundError()) {
            throw PdlPersonIkkeFunnetException("Person ikke funnet i PDL", it)
        } else {
            throw PdlUkjentFeilException("Kall til PDL feilet", it)
        }
    }

    logger.debug("Hentet opphold fra PDL")
    return response.data?.hentPerson?.adressebeskyttelse
}
