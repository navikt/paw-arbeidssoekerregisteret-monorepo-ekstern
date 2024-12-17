package no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils

import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.BadRequestException
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Paging
import java.util.*

fun ApplicationCall.periodeIdParam(): UUID {
    return parameters["periodeId"]?.asUUID() ?: throw BadRequestException("Forespørsel mangler periodeId")
}

fun ApplicationCall.sisteParam(): Boolean {
    return request.queryParameters["siste"]?.toBoolean() ?: false
}

fun ApplicationCall.getPaging(): Paging {
    return if (sisteParam()) Paging(size = 1) else Paging()
}

fun String.asUUID(): UUID = try {
    UUID.fromString(this)
} catch (e: IllegalArgumentException) {
    throw BadRequestException("UUID har feil format")
}
