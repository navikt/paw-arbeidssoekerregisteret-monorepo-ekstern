package no.nav.paw.tilgangskontroll.client

import io.ktor.http.HttpStatusCode
import no.nav.paw.error.model.Data
import no.nav.paw.error.model.ErrorType
import no.nav.paw.error.model.ProblemDetails
import no.nav.paw.error.model.Response
import no.nav.paw.error.model.flatMap

val RESPONSE_DATA_UNIT = Data(Unit)

fun Response<Boolean>.feilVedIkkeTilgang(): Response<Unit> {
    return flatMap { harTilgang ->
        if (harTilgang) RESPONSE_DATA_UNIT
        else {
            ProblemDetails(
                type = ErrorType
                    .domain("tilgangskontroll")
                    .error("ikke_tilgang")
                    .build(),
                status = HttpStatusCode.Forbidden,
                title = "Ikke tilgang",
                detail = "Brukeren har ikke tilgang til denne ressursen.",
                instance = "/tilgangskontroll/har-tilgang"
            )
        }
    }
}