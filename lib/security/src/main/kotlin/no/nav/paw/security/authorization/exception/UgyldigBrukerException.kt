package no.nav.paw.security.authorization.exception

import no.nav.paw.security.common.model.securityErrorType

class UgyldigBrukerException(message: String) :
    AuthorizationException(securityErrorType("ugyldig-bruker"), message)