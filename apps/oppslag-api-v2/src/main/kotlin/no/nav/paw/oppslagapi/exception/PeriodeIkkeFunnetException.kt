package no.nav.paw.oppslagapi.exception

import io.ktor.http.HttpStatusCode
import no.nav.paw.error.exception.ServerResponseException
import no.nav.paw.error.model.ErrorType

class PeriodeIkkeFunnetException(override val message: String) : ServerResponseException(
    status = HttpStatusCode.BadRequest,
    type = ErrorType.domain("perioder").error("periode-ikke-funnet").build(),
    message = message
)