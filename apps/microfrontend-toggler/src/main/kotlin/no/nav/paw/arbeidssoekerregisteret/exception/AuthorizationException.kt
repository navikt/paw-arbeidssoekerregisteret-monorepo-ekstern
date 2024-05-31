package no.nav.paw.arbeidssoekerregisteret.exception

import io.ktor.http.HttpStatusCode

open class AuthorizationException(
    override val code: String,
    override val message: String,
    override val cause: Throwable?
) : ServerResponseException(HttpStatusCode.Forbidden, code, message, cause) {
    constructor(code: String, message: String) : this(code, message, null)
}