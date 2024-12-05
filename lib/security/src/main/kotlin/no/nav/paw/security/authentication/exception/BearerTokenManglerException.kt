package no.nav.paw.security.authentication.exception

import no.nav.paw.security.common.model.securityErrorType

class BearerTokenManglerException(message: String) :
    AuthenticationException(securityErrorType("bearer-token-mangler"), message)