package no.naw.paw.minestillinger.api.vo

import io.ktor.server.plugins.BadRequestException

enum class ApiTjenesteStatus {
    AKTIV,
    INAKTIV,
    OPT_OUT,
    KAN_IKKE_LEVERES
}

fun String?.toApiTjenesteStatus(): ApiTjenesteStatus {
    return when (this) {
        "AKTIV" -> ApiTjenesteStatus.AKTIV
        "INAKTIV" -> ApiTjenesteStatus.INAKTIV
        "OPT_OUT" -> ApiTjenesteStatus.OPT_OUT
        "KAN_IKKE_LEVERES" -> ApiTjenesteStatus.KAN_IKKE_LEVERES
        null -> throw BadRequestException("Tjenestestatus må være satt")
        else -> throw BadRequestException("Ukjent tjenestestatus: $this")
    }
}