package no.nav.paw.security.authorization.exception

import no.nav.paw.error.exception.AuthorizationException
import no.nav.paw.error.model.ErrorTypeBuilder

class IngenTilgangException(message: String) :
    AuthorizationException(ErrorTypeBuilder.builder().domain("security").error("ingen-tilgang").build(), message)