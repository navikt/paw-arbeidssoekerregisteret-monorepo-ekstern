package no.nav.paw.security.authentication.exception

import no.nav.paw.error.exception.AuthenticationException
import no.nav.paw.error.model.ErrorTypeBuilder

class BearerTokenManglerException(message: String) :
    AuthenticationException(
        ErrorTypeBuilder.builder().domain("security").error("bearer-token-mangler").build(),
        message
    )