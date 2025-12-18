package no.nav.paw.oppslagapi.mapping.v4

import io.ktor.http.HttpStatusCode
import no.nav.paw.error.model.Data
import no.nav.paw.error.model.ProblemDetails
import no.nav.paw.error.model.Response
import no.nav.paw.error.model.flatMap
import no.nav.paw.logging.logger.buildApplicationLogger
import no.nav.paw.oppslagapi.exception.PERIODE_IKKE_FUNNET_ERROR_TYPE
import no.nav.paw.oppslagapi.exception.PeriodeIkkeFunnetException
import no.nav.paw.oppslagapi.model.v3.HendelseType
import no.nav.paw.oppslagapi.model.v4.AggregertPeriodeV4
import no.nav.paw.oppslagapi.model.v4.BekreftelseV4
import no.nav.paw.oppslagapi.model.v4.EgenvurderingV4
import no.nav.paw.oppslagapi.model.v4.HendelseV4
import no.nav.paw.oppslagapi.model.v4.OpplysningerOmArbeidssoekerV4
import no.nav.paw.oppslagapi.model.v4.PeriodeAvluttetV4
import no.nav.paw.oppslagapi.model.v4.PeriodeStartetV4
import no.nav.paw.oppslagapi.model.v4.ProfileringV4
import no.nav.paw.oppslagapi.model.v4.TidslinjeV4

private val logger = buildApplicationLogger

fun Response<List<TidslinjeV4>>.finnSistePeriodeV4(): Response<AggregertPeriodeV4> {
    return this.flatMap { tidslinje ->
        val aktivePerioder = tidslinje.filter { it.avsluttet == null }
        when {
            tidslinje.isEmpty() -> {
                ProblemDetails(
                    status = HttpStatusCode.NotFound,
                    type = PERIODE_IKKE_FUNNET_ERROR_TYPE,
                    title = "Not Found",
                    instance = "finnSistePeriode"
                )
            }

            aktivePerioder.size == 1 -> {
                Data(aktivePerioder.first().asAggregertPeriodeV4())
            }

            aktivePerioder.size > 1 -> {
                logger.warn("Flere aktive perioder funnet, bruker nyeste som 'siste'")
                Data(aktivePerioder.maxBy { it.startet }.asAggregertPeriodeV4())
            }

            else -> Data(tidslinje.maxBy { it.startet }.asAggregertPeriodeV4())
        }
    }
}

fun TidslinjeV4.asAggregertPeriodeV4(): AggregertPeriodeV4 {
    val opplysning = this.hendelser.finnOpplysningerV4()
    val profilering = opplysning?.let { this.hendelser.finnProfileringTemp() }
    val egenvurdering = profilering?.let { this.hendelser.finnEgenvurderingTemp() }
    return AggregertPeriodeV4(
        id = this.periodeId,
        identitetsnummer = this.identitetsnummer,
        startet = this.hendelser.finnStartetV4(),
        avsluttet = this.hendelser.finnAvsluttetV4(),
        opplysning = opplysning,
        profilering = profilering,
        egenvurdering = egenvurdering,
        bekreftelse = this.hendelser.finnBekreftelseTemp()
    )
}

private fun List<HendelseV4>.finnStartetV4(): PeriodeStartetV4 {
    return this.firstOrNull { it.type == HendelseType.PERIODE_STARTET_V1 }
        ?.let { it as PeriodeStartetV4 } ?: throw PeriodeIkkeFunnetException("Ingen perioder funnet")
}

fun List<HendelseV4>.finnAvsluttetV4(): PeriodeAvluttetV4? {
    return this.firstOrNull { it.type == HendelseType.PERIODE_AVSLUTTET_V1 }
        ?.let { it as PeriodeAvluttetV4 }
}

fun List<HendelseV4>.finnOpplysningerV4(): OpplysningerOmArbeidssoekerV4? {
    return this.firstOrNull { it.type == HendelseType.OPPLYSNINGER_V4 }
        ?.let { it as OpplysningerOmArbeidssoekerV4 }
}

fun List<HendelseV4>.finnProfileringTemp(): ProfileringV4? {
    return this.firstOrNull { it.type == HendelseType.PROFILERING_V1 }
        ?.let { it as ProfileringV4 }
}

fun List<HendelseV4>.finnEgenvurderingTemp(): EgenvurderingV4? {
    return this.firstOrNull { it.type == HendelseType.EGENVURDERING_V1 }
        ?.let { it as EgenvurderingV4 }
}

fun List<HendelseV4>.finnBekreftelseTemp(): BekreftelseV4? {
    return this.firstOrNull { it.type == HendelseType.BEKREFTELSE_V1 }
        ?.let { it as BekreftelseV4 }
}