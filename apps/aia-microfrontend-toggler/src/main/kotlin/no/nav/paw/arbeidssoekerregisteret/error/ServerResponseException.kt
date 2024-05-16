package no.nav.paw.arbeidssoekerregisteret.error

import io.ktor.http.HttpStatusCode

open class ServerResponseException(
    val status: HttpStatusCode,
    override val code: String,
    override val message: String,
    override val cause: Throwable?
) : ErrorCodeAwareException(code, message, cause) {
}