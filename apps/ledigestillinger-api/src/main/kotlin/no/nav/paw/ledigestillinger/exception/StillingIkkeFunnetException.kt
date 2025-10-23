package no.nav.paw.ledigestillinger.exception

import io.ktor.http.HttpStatusCode
import no.nav.paw.error.exception.ServerResponseException
import no.nav.paw.error.model.ErrorType

val STILLING_IKKE_FUNNET_ERROR_TYPE = ErrorType.domain("stillinger").error("ikke-funnet").build()

class StillingIkkeFunnetException : ServerResponseException(
    status = HttpStatusCode.NotFound,
    type = STILLING_IKKE_FUNNET_ERROR_TYPE,
    message = "Stilling ikke funnet"
)