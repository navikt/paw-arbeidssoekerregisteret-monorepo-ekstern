package no.nav.paw.oppslagapi.exception

import io.ktor.http.HttpStatusCode
import no.nav.paw.error.exception.ServerResponseException
import no.nav.paw.error.model.ErrorType
import java.net.URI

val UGYLDIG_FORESPOERSEL_ERROR_TYPE: URI = ErrorType.domain("http").error("ugyldig-forespoersel").build()

class UgyldigForespoerselException(override val message: String) : ServerResponseException(
    status = HttpStatusCode.BadRequest,
    type = UGYLDIG_FORESPOERSEL_ERROR_TYPE,
    message = message
)