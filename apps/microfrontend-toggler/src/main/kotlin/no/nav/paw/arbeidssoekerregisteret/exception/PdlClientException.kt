package no.nav.paw.arbeidssoekerregisteret.exception

import io.ktor.http.HttpStatusCode

class PdlClientException(
    override val message: String,
    override val cause: Throwable?
) : ClientResponseException(HttpStatusCode.InternalServerError, "PDL_SVARTE_MED_FEIL", message, cause) {
    constructor(message: String) : this(message, null)
}