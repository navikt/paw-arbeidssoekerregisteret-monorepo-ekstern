package no.nav.paw.pdl.client

import no.nav.paw.pdl.exception.PdlPersonIkkeFunnetException
import no.nav.paw.pdl.exception.PdlUkjentFeilException
import no.nav.paw.pdl.graphql.generated.HentIdenter
import no.nav.paw.pdl.graphql.generated.hentidenter.IdentInformasjon

suspend fun PdlClient.hentIdenter(
    ident: String,
    historikk: Boolean = false,
    callId: String?,
    navConsumerId: String?,
    behandlingsnummer: String,
): List<IdentInformasjon> {
    val query = HentIdenter(HentIdenter.Variables(ident = ident, historisk = historikk))

    logger.debug("Henter identer fra PDL")

    val response = execute(
        query = query,
        callId = callId,
        navConsumerId = navConsumerId,
        behandlingsnummer = behandlingsnummer,
    )

    response.errors?.let {
        logger.debug("Henter identer fra PDL feilet med: {}", it)
        if (response.hasNotFoundError()) {
            throw PdlPersonIkkeFunnetException("Person ikke funnet i PDL", it)
        } else {
            throw PdlUkjentFeilException("Kall til PDL feilet", it)
        }
    }

    logger.debug("Hentet identer fra PDL")

    return response
        .data
        ?.hentIdenter
        ?.identer ?: emptyList()
}
