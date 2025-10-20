package no.naw.paw.minestillinger.domain

import io.ktor.server.plugins.BadRequestException
import no.naw.paw.minestillinger.api.vo.ApiTjenesteStatus

enum class TjenesteStatus {
    AKTIV,
    INAKTIV,
    OPT_OUT,
    KAN_IKKE_LEVERES
}

fun TjenesteStatus.feilVedForsøkPåÅSetteKanIkkeLeveres(): TjenesteStatus {
    return if (this == TjenesteStatus.KAN_IKKE_LEVERES) {
        throw BadRequestException("Kan ikke sette tjenestestatus til KAN_IKKE_LEVERES manuelt")
    } else this
}

fun ApiTjenesteStatus.toTjenesteStatus(): TjenesteStatus {
    return when (this) {
        ApiTjenesteStatus.AKTIV -> TjenesteStatus.AKTIV
        ApiTjenesteStatus.INAKTIV -> TjenesteStatus.INAKTIV
        ApiTjenesteStatus.OPT_OUT -> TjenesteStatus.OPT_OUT
        ApiTjenesteStatus.KAN_IKKE_LEVERES -> TjenesteStatus.KAN_IKKE_LEVERES
    }
}