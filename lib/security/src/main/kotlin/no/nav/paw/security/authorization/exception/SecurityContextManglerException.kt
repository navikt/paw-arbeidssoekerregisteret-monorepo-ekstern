package no.nav.paw.security.authorization.exception

import no.nav.paw.security.common.model.securityErrorType

class SecurityContextManglerException(message: String) :
    AuthorizationException(securityErrorType("security-context-mangler"), message)