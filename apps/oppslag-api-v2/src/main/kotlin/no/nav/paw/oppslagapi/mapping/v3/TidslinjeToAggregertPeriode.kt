package no.nav.paw.oppslagapi.mapping.v3

import io.ktor.http.HttpStatusCode
import io.opentelemetry.api.trace.Span
import no.nav.paw.error.model.Data
import no.nav.paw.error.model.ProblemDetails
import no.nav.paw.error.model.Response
import no.nav.paw.error.model.flatMap
import no.nav.paw.error.model.map
import no.nav.paw.logging.logger.buildApplicationLogger
import no.nav.paw.oppslagapi.exception.PERIODE_IKKE_FUNNET_ERROR_TYPE
import no.nav.paw.oppslagapi.exception.PeriodeIkkeFunnetException
import no.nav.paw.oppslagapi.model.v3.AggregertPeriode
import no.nav.paw.oppslagapi.model.v3.Bekreftelse
import no.nav.paw.oppslagapi.model.v3.Egenvurdering
import no.nav.paw.oppslagapi.model.v3.Hendelse
import no.nav.paw.oppslagapi.model.v3.HendelseType
import no.nav.paw.oppslagapi.model.v3.OpplysningerOmArbeidssoeker
import no.nav.paw.oppslagapi.model.v3.PeriodeAvluttet
import no.nav.paw.oppslagapi.model.v3.PeriodeStartet
import no.nav.paw.oppslagapi.model.v3.Profilering
import no.nav.paw.oppslagapi.model.v3.Tidslinje

private val logger = buildApplicationLogger

fun Response<List<Tidslinje>>.finnSistePeriode(): Response<AggregertPeriode> {
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
                Data(aktivePerioder.first().asAggregertPeriode())
            }

            aktivePerioder.size > 1 -> {
                logger.warn("Flere aktive perioder funnet, bruker nyeste som 'siste'")
                Data(aktivePerioder.maxBy { it.startet }.asAggregertPeriode())
            }

            else -> Data(tidslinje.maxBy { it.startet }.asAggregertPeriode())
        }
    }
}

fun Tidslinje.asAggregertPeriode(): AggregertPeriode {
    val opplysning = this.hendelser.finnOpplysninger()
    val profilering = opplysning?.let { this.hendelser.finnProfilering() }
    val egenvurdering = profilering?.let { this.hendelser.finnEgenvurdering() }
    return AggregertPeriode(
        id = this.periodeId,
        identitetsnummer = this.identitetsnummer,
        startet = this.hendelser.finnStartet(),
        avsluttet = this.hendelser.finnAvsluttet(),
        opplysning = opplysning,
        profilering = profilering,
        egenvurdering = egenvurdering,
        bekreftelse = this.hendelser.finnBekreftelse()
    )
}

fun List<Hendelse>.finnStartet(): PeriodeStartet {
    return this.firstOrNull { it.type == HendelseType.PERIODE_STARTET_V1 }
        ?.let { it as PeriodeStartet } ?: throw PeriodeIkkeFunnetException("Ingen perioder funnet")
}

fun List<Hendelse>.finnAvsluttet(): PeriodeAvluttet? {
    return this.firstOrNull { it.type == HendelseType.PERIODE_AVSLUTTET_V1 }
        ?.let { it as PeriodeAvluttet }
}

fun List<Hendelse>.finnOpplysninger(): OpplysningerOmArbeidssoeker? {
    return this.firstOrNull { it.type == HendelseType.OPPLYSNINGER_V4 }
        ?.let { it as OpplysningerOmArbeidssoeker }
}

fun List<Hendelse>.finnProfilering(): Profilering? {
    return this.firstOrNull { it.type == HendelseType.PROFILERING_V1 }
        ?.let { it as Profilering }
}

fun List<Hendelse>.finnEgenvurdering(): Egenvurdering? {
    return this.firstOrNull { it.type == HendelseType.EGENVURDERING_V1 }
        ?.let { it as Egenvurdering }
}

fun List<Hendelse>.finnBekreftelse(): Bekreftelse? {
    return this.firstOrNull { it.type == HendelseType.BEKREFTELSE_V1 }
        ?.let { it as Bekreftelse }
}