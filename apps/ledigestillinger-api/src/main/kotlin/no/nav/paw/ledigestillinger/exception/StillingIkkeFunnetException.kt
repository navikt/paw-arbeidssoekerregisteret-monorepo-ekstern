package no.nav.paw.ledigestillinger.exception

import io.ktor.http.HttpStatusCode
import no.nav.paw.error.exception.ServerResponseException
import no.nav.paw.error.model.ErrorType

class StillingIkkeFunnetException : ServerResponseException(
    status = HttpStatusCode.NotFound,
    type = ErrorType.domain("stillinger").error("ikke-funnet").build(),
    message = "Stilling ikke funnet"
)