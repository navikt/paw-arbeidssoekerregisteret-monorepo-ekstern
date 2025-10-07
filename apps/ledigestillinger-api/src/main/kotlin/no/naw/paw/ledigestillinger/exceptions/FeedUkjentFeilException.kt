package no.naw.paw.ledigestillinger.exceptions

import io.ktor.http.HttpStatusCode
import no.nav.paw.error.exception.ClientResponseException
import no.nav.paw.error.model.ErrorType

val FEED_UKJENT_FEIL_ERROR_TYPE = ErrorType.team("pam").domain("stillinger").error("ukjent-feil").build()

class FeedUkjentFeilException(
    override val status: HttpStatusCode,
    override val message: String
) : ClientResponseException(
    status = status,
    type = FEED_UKJENT_FEIL_ERROR_TYPE,
    message = message
)