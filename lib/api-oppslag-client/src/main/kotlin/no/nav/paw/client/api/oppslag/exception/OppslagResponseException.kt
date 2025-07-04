package no.nav.paw.client.api.oppslag.exception

import io.ktor.http.HttpStatusCode
import no.nav.paw.error.exception.ClientResponseException
import no.nav.paw.error.model.ErrorType

class PerioderOppslagResponseException(
    override val status: HttpStatusCode,
    override val message: String
) : ClientResponseException(
    status = status,
    type = ErrorType.domain("perioder").error("error-response").build(),
    message = message,
)

class ArbeidssoekerperioderAggregertOppslagResponseException(
    override val status: HttpStatusCode,
    override val message: String
) : ClientResponseException(
    status = status,
    type = ErrorType.domain("arbeidssøkerperioder-aggregert").error("error-response").build(),
    message = message,
)

class ProfileringOppslagResponseException(
    override val status: HttpStatusCode,
    override val message: String
) : ClientResponseException(
    status = status,
    type = ErrorType.domain("profilering").error("error-response").build(),
    message = message,
)

class EgenvurderingOppslagResponseException(
    override val status: HttpStatusCode,
    override val message: String
) : ClientResponseException(
    status = status,
    type = ErrorType.domain("egenvurdering").error("error-response").build(),
    message = message,
)