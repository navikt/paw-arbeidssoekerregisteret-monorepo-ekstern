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
import no.nav.paw.oppslagapi.model.v3.HendelseType
import no.nav.paw.oppslagapi.model.v3.SortOrder
import no.nav.paw.oppslagapi.model.v3.Tidslinje
import no.nav.paw.security.authentication.model.SecurityContext

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
    ordering: SortOrder // TODO: Sortere hendelser også?
): Tidslinje {
    return if (types.isEmpty()) {
        this
    } else {
        this.copy(
            hendelser = hendelser.filter { types.contains(it.type) }
        )
    }
}

fun Iterable<Tidslinje>.sortedByStart(ordering: SortOrder): List<Tidslinje> {
    return when (ordering) {
        SortOrder.ASC -> this.sortedBy { it.startet.toEpochMilli() }
        SortOrder.DESC -> this.sortedByDescending { it.startet.toEpochMilli() }
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