package no.nav.paw.oppslagapi.exception

import io.ktor.http.HttpStatusCode
import no.nav.paw.error.exception.ServerResponseException
import no.nav.paw.error.model.ErrorType
import java.net.URI

val PERIODE_IKKE_FUNNET_ERROR_TYPE: URI = ErrorType.domain("perioder").error("periode-ikke-funnet").build()

class PeriodeIkkeFunnetException(override val message: String) : ServerResponseException(
    status = HttpStatusCode.NotFound,
    type = PERIODE_IKKE_FUNNET_ERROR_TYPE,
    message = message
)