package no.naw.paw.minestillinger

import io.ktor.http.HttpStatusCode
import no.nav.paw.error.model.ErrorType
import no.nav.paw.error.model.ProblemDetails
import java.util.*


private const val domain = "mine-stillinger"

fun ugyldigVerdiIRequest(
    felt: String,
    verdi: String
): ProblemDetails {
    return ProblemDetails(
        id = UUID.randomUUID(),
        type = ErrorType.default().error("ugyldig-verdi").domain(domain).build(),
        title = "Oppdatering av tjenestestatus ikke tillatt",
        detail = "Ugyldig verdi '$verdi' for feltet '$felt'",
        status = HttpStatusCode.BadRequest,
        instance = "api"
    )
}

fun oppdateringIkkeTillatt(detail: String): ProblemDetails {
    return ProblemDetails(
        id = UUID.randomUUID(),
        type = ErrorType.default().error("oppdatering-ikke-tillatt").domain(domain).build(),
        title = "Oppdatering av tjenestestatus ikke tillatt",
        detail = detail,
        status = HttpStatusCode.Forbidden,
        instance = "api_kan_sette_tjenestestatus"
    )
}

fun brukerIkkeFunnet(): ProblemDetails {
    return ProblemDetails(
        id = UUID.randomUUID(),
        type = ErrorType.default().error("bruker-ikke-funnet").domain(domain).build(),
        title = "Bruker ikke funnet",
        detail = null,
        status = HttpStatusCode.NotFound,
        instance = "brukerprofil"
    )
}

fun tjenesteIkkeAktiv(): ProblemDetails {
    return ProblemDetails(
        id = UUID.randomUUID(),
        type = ErrorType.default().error("tjeneste-ikke-aktiv").domain(domain).build(),
        title = "Tjenesten er ikke aktiv for denne brukeren",
        detail = null,
        status = HttpStatusCode.Forbidden,
        instance = "tjeneste-ikke-aktiv"
    )
}

fun reiseveiSøkIkkeStøttet(): ProblemDetails {
    return ProblemDetails(
        id = UUID.randomUUID(),
        type = ErrorType.default().error("ikke-støttet").domain(domain).build(),
        title = "Funksjonen er ikke støttet",
        detail = "Funksjonen 'reisevei-søk' er ikke støttet.",
        status = HttpStatusCode.NotImplemented,
        instance = "api"
    )
}

fun internFeil(detail: String): ProblemDetails {
    return ProblemDetails(
        id = UUID.randomUUID(),
        type = ErrorType.default().error("intern-feil").domain(domain).build(),
        title = "Intern feil",
        detail = detail,
        status = HttpStatusCode.InternalServerError,
        instance = "intern-feil"
    )
}



