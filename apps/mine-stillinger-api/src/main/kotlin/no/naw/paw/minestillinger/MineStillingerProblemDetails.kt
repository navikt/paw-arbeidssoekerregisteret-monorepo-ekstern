package no.naw.paw.minestillinger

import io.ktor.http.HttpStatusCode
import no.nav.paw.error.exception.ProblemDetailsException
import no.nav.paw.error.model.ErrorType
import no.nav.paw.error.model.ProblemDetails

fun throwProblemDetailsException(
    error: Error,
    detail: String,
    instance: String = "/api/v1/",
    cause: Throwable? = null
): Nothing {
    val pd = mineStillingerProblemDetails(
        error = error,
        detail = detail,
        instance = instance,
    )
    throw ProblemDetailsException(pd)
}

fun mineStillingerProblemDetails(
    error: Error,
    detail: String,
    instance: String = "/api/v1/",
): ProblemDetails {
    return ProblemDetails(
        type = ErrorType
            .domain("mine_stillinger.${error.domain}")
            .error(error.domain.lowercase())
            .build(),
        title = error.title,
        status = error.httpStatus,
        detail = detail,
        instance = instance
    )
}

enum class Error(
    val domain: String,
    val httpStatus: HttpStatusCode,
    val title: String
) {
    BRUKERPROFIL_IKKE_FUNNET(
        domain = "brukerprofil",
        title = "Brukerprofil ikke funnet",
        httpStatus = HttpStatusCode.NotFound
    ),
    AKTIVERING_IKKE_TILLAT(
        domain = "brukerprofil_tjenestestatus",
        title = "Tjenesten kan ikke aktiveres for denne brukeren",
        httpStatus = HttpStatusCode.Forbidden
    ),
    KAN_IKKE_SETTES_VIA_API(
        domain = "brukerprofil_tjenestestatus",
        title = "Oppgitt tjenestestatus kan ikke settes",
        httpStatus = HttpStatusCode.Forbidden
    ),
    TJENESTEN_ER_IKKE_AKTIV(
        domain = "brukerprofil_tjenestestatus",
        title = "Tjenesten er ikke aktiv for denne brukeren",
        httpStatus = HttpStatusCode.Forbidden
    ),
}