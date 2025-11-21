package no.nav.paw.oppslagapi.routes.v3

import io.ktor.server.application.ApplicationCall
import no.nav.paw.error.model.Data
import no.nav.paw.error.model.Response
import no.nav.paw.error.model.map
import no.nav.paw.logging.logger.buildApplicationLogger
import no.nav.paw.oppslagapi.data.query.ApplicationQueryLogic
import no.nav.paw.oppslagapi.data.query.PeriodeUtenStartHendelseException
import no.nav.paw.oppslagapi.exception.UgyldigForespoerselException
import no.nav.paw.oppslagapi.mapping.v3.asV3
import no.nav.paw.oppslagapi.model.v2.V2BaseRequest
import no.nav.paw.oppslagapi.model.v2.hentTidslinjer
import no.nav.paw.oppslagapi.model.v3.Bekreftelse
import no.nav.paw.oppslagapi.model.v3.Egenvurdering
import no.nav.paw.oppslagapi.model.v3.Hendelse
import no.nav.paw.oppslagapi.model.v3.HendelseType
import no.nav.paw.oppslagapi.model.v3.OpplysningerOmArbeidssoeker
import no.nav.paw.oppslagapi.model.v3.PaaVegneAvStart
import no.nav.paw.oppslagapi.model.v3.PaaVegneAvStopp
import no.nav.paw.oppslagapi.model.v3.PeriodeAvluttet
import no.nav.paw.oppslagapi.model.v3.PeriodeStartet
import no.nav.paw.oppslagapi.model.v3.Profilering
import no.nav.paw.oppslagapi.model.v3.SortOrder
import no.nav.paw.oppslagapi.model.v3.Tidslinje
import no.nav.paw.security.authentication.model.SecurityContext
import java.time.Duration
import java.time.Instant

private val logger = buildApplicationLogger

suspend fun ApplicationQueryLogic.finnTidslinjer(
    securityContext: SecurityContext,
    request: V2BaseRequest,
    types: List<HendelseType> = emptyList(),
    ordering: SortOrder = SortOrder.DESC
): Response<List<Tidslinje>> {
    return try {
        hentTidslinjer(
            securityContext = securityContext,
            baseRequest = request
        ).map { tidslinjer ->
            tidslinjer
                .map { it.asV3().byTypes(types, ordering) }
                .sortedByStart(ordering)
        }
    } catch (e: PeriodeUtenStartHendelseException) {
        logger.debug("Periode uten start-hendelse", e)
        Data(emptyList())
    }
}

private fun Tidslinje.byTypes(
    types: List<HendelseType>,
    ordering: SortOrder
): Tidslinje {
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

fun Iterable<Tidslinje>.sortedByStart(ordering: SortOrder): List<Tidslinje> {
    return when (ordering) {
        SortOrder.ASC -> this.sortedBy { it.startet.toEpochMilli() }
        SortOrder.DESC -> this.sortedByDescending { it.startet.toEpochMilli() }
    }
}

fun Iterable<Hendelse>.sortedByTidspunk(ordering: SortOrder): List<Hendelse> {
    return when (ordering) {
        SortOrder.ASC -> this.sortedBy { it.tidspunkt().toEpochMilli() }
        SortOrder.DESC -> this.sortedByDescending { it.tidspunkt().toEpochMilli() }
    }
}

fun Hendelse.tidspunkt(): Instant {
    return when (this) {
        is PeriodeStartet -> this.tidspunkt
        is PeriodeAvluttet -> this.tidspunkt
        is OpplysningerOmArbeidssoeker -> this.sendtInnAv.tidspunkt
        is Profilering -> this.sendtInnAv.tidspunkt
        is Egenvurdering -> this.sendtInnAv.tidspunkt
        is Bekreftelse -> this.svar.sendtInnAv.tidspunkt
        is PaaVegneAvStart -> Instant.EPOCH
        is PaaVegneAvStopp -> Instant.EPOCH + Duration.ofMinutes(1)
    }
}

fun ApplicationCall.types(): List<HendelseType> {
    val types = request.queryParameters["types"]
    return if (types.isNullOrEmpty()) {
        emptyList()
    } else {
        try {
            types.split(",")
                .map { HendelseType.valueOf(it) }
        } catch (e: IllegalArgumentException) {
            logger.trace("Ugyldige types: $types", e)
            throw UgyldigForespoerselException("Ugyldige types: $types")
        }
    }
}

fun ApplicationCall.ordering(): SortOrder {
    val ordering = request.queryParameters["ordering"]
    return if (ordering.isNullOrEmpty()) {
        SortOrder.DESC
    } else {
        try {
            SortOrder.valueOf(ordering)
        } catch (e: IllegalArgumentException) {
            logger.trace("Ugyldig ordering: $ordering", e)
            throw UgyldigForespoerselException("Ugyldig ordering: $ordering")
        }
    }
}