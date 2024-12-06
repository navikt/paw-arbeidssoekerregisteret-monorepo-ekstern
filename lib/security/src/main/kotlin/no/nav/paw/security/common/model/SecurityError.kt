package no.nav.paw.security.common.model

import no.nav.paw.error.model.ErrorType
import java.net.URI

fun securityErrorType(error: String): URI = ErrorType.domain("security").error(error).build()
