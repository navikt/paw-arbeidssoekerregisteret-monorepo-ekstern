package no.nav.paw.oppslagapi.routes.v1

import io.ktor.server.application.ApplicationCall
import kotlin.text.toBoolean

fun ApplicationCall.bareReturnerSiste(): Boolean {
    return request.queryParameters["siste"]?.toBoolean() ?: false
}
