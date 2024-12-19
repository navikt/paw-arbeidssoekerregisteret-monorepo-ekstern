package no.nav.paw.pdl.model

import no.nav.paw.error.model.ErrorType
import java.net.URI

fun pdlErrorType(error: String): URI = ErrorType.domain("pdl").error(error).build()