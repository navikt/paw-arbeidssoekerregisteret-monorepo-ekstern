package no.naw.paw.minestillinger.brukerprofil

import io.ktor.http.HttpStatusCode
import no.nav.paw.error.model.ErrorType
import no.nav.paw.error.model.ProblemDetails
import java.util.*


fun oppdateringIkkeTillatt(detail: String): ProblemDetails {
    return ProblemDetails(
        id = UUID.randomUUID(),
        type = ErrorType.default().error("oppdatering-ikke-tillatt").domain("mine-stillinger").build(),
        title = "Oppdatering av tjenestestatus ikke tillatt",
        detail = detail,
        status = HttpStatusCode.Forbidden,
        instance = "api_kan_sette_tjenestestatus"
    )
}

fun brukerIkkeFunnet(): ProblemDetails {
    return ProblemDetails(
        id = UUID.randomUUID(),
        type = ErrorType.default().error("bruker-ikke-funnet").domain("mine-stillinger").build(),
        title = "Bruker ikke funnet",
        detail = null,
        status = HttpStatusCode.NotFound,
        instance = "brukerprofil"
    )
}

fun internFeil(detail: String): ProblemDetails {
    return ProblemDetails(
        id = UUID.randomUUID(),
        type = ErrorType.default().error("intern-feil").domain("mine-stillinger").build(),
        title = "Intern feil",
        detail = null,
        status = HttpStatusCode.InternalServerError,
        instance = "intern-feil"
    )
}



