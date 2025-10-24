package no.nav.paw.pdl.client

import no.nav.paw.pdl.exception.PdlPersonIkkeFunnetException
import no.nav.paw.pdl.exception.PdlUkjentFeilException
import no.nav.paw.pdl.graphql.generated.HentAdressebeskyttelse
import no.nav.paw.pdl.graphql.generated.HentAdressebeskyttelseBolk
import no.nav.paw.pdl.graphql.generated.hentadressebeskyttelse.Adressebeskyttelse
import no.nav.paw.pdl.graphql.generated.hentadressebeskyttelsebolk.HentPersonBolkResult

suspend fun PdlClient.hentAdressebeskyttelse(
    ident: String,
    navConsumerId: String?,
    behandlingsnummer: String,
): List<Adressebeskyttelse>? {
    val query = HentAdressebeskyttelse(HentAdressebeskyttelse.Variables(ident = ident))
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

    return response.data?.hentPerson?.adressebeskyttelse
}
suspend fun PdlClient.hentAdressebeskyttelse(
    identer: List<String>,
    navConsumerId: String?,
    behandlingsnummer: String
): List <HentPersonBolkResult>? {
    val query = HentAdressebeskyttelseBolk(HentAdressebeskyttelseBolk.Variables(identer = identer))
    val response = execute(
        query = query,
        callId = null,
        navConsumerId = navConsumerId,
        behandlingsnummer = behandlingsnummer
    )
    response.errors?.let {
        if (response.hasNotFoundError()) {
            throw PdlPersonIkkeFunnetException("Person ikke funnet i PDL", it)
        } else {
            throw PdlUkjentFeilException("Kall til PDL feilet", it)
        }
    }
    return response.data?.hentPersonBolk
}

