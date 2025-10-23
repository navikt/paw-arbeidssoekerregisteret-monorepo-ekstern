package no.nav.paw.ledigestillinger.exception

import io.ktor.http.HttpStatusCode
import no.nav.paw.error.exception.ServerResponseException
import no.nav.paw.error.model.ErrorType

val MALFORMED_REQUEST_ERROR_TYPE = ErrorType.domain("stillinger").error("malformed-request").build()

class MalformedRequestException(override val message: String) : ServerResponseException(
    status = HttpStatusCode.BadRequest,
    type = MALFORMED_REQUEST_ERROR_TYPE,
    message = message
)