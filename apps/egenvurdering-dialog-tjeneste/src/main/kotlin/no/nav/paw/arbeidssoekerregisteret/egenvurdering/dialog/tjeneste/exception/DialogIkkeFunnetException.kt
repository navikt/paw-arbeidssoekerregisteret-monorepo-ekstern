package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.exception

import io.ktor.http.HttpStatusCode
import no.nav.paw.error.exception.ServerResponseException
import no.nav.paw.error.model.ErrorType

val DIALOG_IKKE_FUNNET_ERROR_TYPE = ErrorType.domain("egenvurdering").error("dialog-ikke-funnet").build()

class DialogIkkeFunnetException : ServerResponseException(
    status = HttpStatusCode.NotFound,
    type = DIALOG_IKKE_FUNNET_ERROR_TYPE,
    message = "Dialog ikke funnet for arbeidssøkerperiode"
)