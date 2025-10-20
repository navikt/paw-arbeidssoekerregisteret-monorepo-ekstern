package no.nav.paw.ledigestillinger.exception

import io.ktor.http.HttpStatusCode
import no.nav.paw.error.exception.ServerResponseException
import no.nav.paw.error.model.ErrorType

class RequestParamMissingException(override val message: String) : ServerResponseException(
    status = HttpStatusCode.BadRequest,
    type = ErrorType.domain("stillinger").error("request-param-missing").build(),
    message = message
)