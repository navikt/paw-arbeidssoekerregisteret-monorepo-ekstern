package no.nav.paw.arbeidssoekerregisteret.exception

import no.nav.paw.error.exception.AuthorizationException

class BrukerHarIkkeTilgangException(message: String) :
    AuthorizationException("PAW_BRUKER_HAR_IKKE_TILGANG", message, null)