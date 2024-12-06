package no.nav.paw.security.authorization.exception

import no.nav.paw.security.common.model.securityErrorType

class UgyldigBearerTokenException(message: String) :
    AuthorizationException(securityErrorType("ugyldig-bearer-token"), message)