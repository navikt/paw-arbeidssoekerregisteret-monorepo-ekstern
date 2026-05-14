package no.nav.paw.oppslagapi.exception

import io.ktor.http.HttpStatusCode
import no.nav.paw.error.exception.ServerResponseException
import no.nav.paw.error.model.ErrorType

class FinnIdentiteterFeiletException(override val message: String, cause: Throwable) : ServerResponseException(
    status = HttpStatusCode.InternalServerError,
    type = ErrorType.domain("identiteter").error("feil-ved-henting").build(),
    message = message,
    cause = cause
)