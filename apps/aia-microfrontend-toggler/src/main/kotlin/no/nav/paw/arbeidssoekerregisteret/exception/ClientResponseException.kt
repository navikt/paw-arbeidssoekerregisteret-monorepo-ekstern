package no.nav.paw.arbeidssoekerregisteret.exception

import io.ktor.http.HttpStatusCode

open class ClientResponseException(
    val status: HttpStatusCode,
    override val code: String,
    override val message: String,
    override val cause: Throwable?
) : ErrorCodeAwareException(code, message, cause)