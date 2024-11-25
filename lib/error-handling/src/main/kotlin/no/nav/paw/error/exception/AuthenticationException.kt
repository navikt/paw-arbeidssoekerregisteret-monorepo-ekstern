package no.nav.paw.error.exception

import io.ktor.http.HttpStatusCode
import java.net.URI

open class AuthenticationException(
    override val type: URI,
    override val message: String,
    override val cause: Throwable? = null
) : ServerResponseException(HttpStatusCode.Unauthorized, type, message, cause)