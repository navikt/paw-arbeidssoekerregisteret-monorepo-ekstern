package no.nav.paw.arbeidssoekerregisteret.error

open class ErrorCodeAwareException(open val code: String, override val message: String, override val cause: Throwable?) :
    Exception(message, cause) {

    constructor(code: String, message: String) : this(code, message, null)
}