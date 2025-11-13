package no.nav.paw.oppslagapi.mapping

import no.nav.paw.error.exception.ServerResponseException
import no.nav.paw.error.model.ProblemDetails

fun ServerResponseException.asProblemDetails(
    instance: String
): ProblemDetails = ProblemDetails(
    type = this.type,
    status = this.status,
    title = this.status.description,
    detail = this.message,
    instance = instance
)