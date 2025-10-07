package no.naw.paw.ledigestillinger.exceptions

import io.ktor.http.HttpStatusCode
import no.nav.paw.error.exception.ClientResponseException
import no.nav.paw.error.model.ErrorType

val FEED_NOT_FOUND_ERROR_TYPE = ErrorType.team("pam").domain("stillinger").error("feed-ikke-funnet").build()

class FeedIkkeFunnetException(override val message: String) : ClientResponseException(
    status = HttpStatusCode.NotFound,
    type = FEED_NOT_FOUND_ERROR_TYPE,
    message = message
)