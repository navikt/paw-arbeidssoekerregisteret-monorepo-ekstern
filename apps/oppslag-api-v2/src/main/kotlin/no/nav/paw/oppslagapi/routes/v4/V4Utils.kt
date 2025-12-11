package no.nav.paw.oppslagapi.routes.v4

import no.nav.paw.error.model.Data
import no.nav.paw.error.model.Response
import no.nav.paw.error.model.map
import no.nav.paw.logging.logger.buildApplicationLogger
import no.nav.paw.oppslagapi.data.query.ApplicationQueryLogic
import no.nav.paw.oppslagapi.data.query.PeriodeUtenStartHendelseException
import no.nav.paw.oppslagapi.mapping.v4.asV4
import no.nav.paw.oppslagapi.model.v3.HendelseType
import no.nav.paw.oppslagapi.model.v3.IdentitetsnummerQueryRequest
import no.nav.paw.oppslagapi.model.v3.PerioderQueryRequest
import no.nav.paw.oppslagapi.model.v3.QueryRequest
import no.nav.paw.oppslagapi.model.v3.SortOrder
import no.nav.paw.oppslagapi.model.v4.BekreftelseV4
import no.nav.paw.oppslagapi.model.v4.EgenvurderingV4
import no.nav.paw.oppslagapi.model.v4.HendelseV4
import no.nav.paw.oppslagapi.model.v4.OpplysningerOmArbeidssoekerV4
import no.nav.paw.oppslagapi.model.v4.PaaVegneAvStartV4
import no.nav.paw.oppslagapi.model.v4.PaaVegneAvStoppV4
import no.nav.paw.oppslagapi.model.v4.PeriodeAvluttetV4
import no.nav.paw.oppslagapi.model.v4.PeriodeStartetV4
import no.nav.paw.oppslagapi.model.v4.ProfileringV4
import no.nav.paw.oppslagapi.model.v4.TidslinjeV4
import no.nav.paw.security.authentication.model.SecurityContext
import java.time.Duration
import java.time.Instant

private val logger = buildApplicationLogger

suspend fun ApplicationQueryLogic.finnTidslinjerV4(
    securityContext: SecurityContext,
    request: QueryRequest,
    types: List<HendelseType> = emptyList(),
    ordering: SortOrder = SortOrder.DESC
): Response<List<TidslinjeV4>> {
    return try {
        when (request) {
            is IdentitetsnummerQueryRequest -> {
                hentTidslinjer(
                    securityContext = securityContext,
                    identitetsnummer = request.identitetsnummer
                )
            }

            is PerioderQueryRequest -> {
                hentTidslinjer(
                    securityContext = securityContext,
                    perioder = request.perioder
                )
            }
        }.map { tidslinjer ->
            tidslinjer
                .map { it.asV4().byTypes(types, ordering) }
                .sortedByStart(ordering)
        }
    } catch (e: PeriodeUtenStartHendelseException) {
        logger.debug("Periode uten start-hendelse", e)
        Data(emptyList())
    }
}

private fun TidslinjeV4.byTypes(
    types: List<HendelseType>,
    ordering: SortOrder
): TidslinjeV4 {
    return if (types.isEmpty()) {
        this.copy(
            hendelser = hendelser
                .sortedByTidspunk(ordering)
        )
    } else {
        this.copy(
            hendelser = hendelser
                .filter { types.contains(it.type) }
                .sortedByTidspunk(ordering)
        )
    }
}

private fun Iterable<TidslinjeV4>.sortedByStart(ordering: SortOrder): List<TidslinjeV4> {
    return when (ordering) {
        SortOrder.ASC -> this.sortedBy { it.startet.toEpochMilli() }
        SortOrder.DESC -> this.sortedByDescending { it.startet.toEpochMilli() }
    }
}

fun Iterable<HendelseV4>.sortedByTidspunk(ordering: SortOrder): List<HendelseV4> {
    return when (ordering) {
        SortOrder.ASC -> this.sortedBy { it.tidspunkt().toEpochMilli() }
        SortOrder.DESC -> this.sortedByDescending { it.tidspunkt().toEpochMilli() }
    }
}

private fun HendelseV4.tidspunkt(): Instant {
    return when (this) {
        is PeriodeStartetV4 -> this.tidspunkt
        is PeriodeAvluttetV4 -> this.tidspunkt
        is OpplysningerOmArbeidssoekerV4 -> this.sendtInnAv.tidspunkt
        is ProfileringV4 -> this.sendtInnAv.tidspunkt
        is EgenvurderingV4 -> this.sendtInnAv.tidspunkt
        is BekreftelseV4 -> this.svar.sendtInnAv.tidspunkt
        is PaaVegneAvStartV4 -> Instant.EPOCH
        is PaaVegneAvStoppV4 -> Instant.EPOCH + Duration.ofMinutes(1)
    }
}
