package no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.BekreftelseFunctions
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.BekreftelseRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Paging
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.buildLogger
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.security.authentication.model.Identitetsnummer
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class BekreftelseRepository {
    private val logger = buildLogger

    fun finnBekreftelserForPeriodeIdList(
        periodeIdList: Collection<UUID>,
        paging: Paging = Paging()
    ): List<BekreftelseRow> =
        transaction {
            val rows = BekreftelseFunctions.findForPeriodeIdList(periodeIdList, paging)
            if (paging.ordering == SortOrder.ASC) {
                rows.sortedBy { it.svar.gjelderFra }.take(paging.size)
            } else {
                rows.sortedByDescending { it.svar.gjelderFra }.take(paging.size)
            }
        }

    fun finnBekreftelserForIdentitetsnummerList(
        identitetsnummerList: Collection<Identitetsnummer>,
        paging: Paging = Paging()
    ): List<BekreftelseRow> =
        transaction {
            val rows = BekreftelseFunctions.findForIdentitetsnummerList(identitetsnummerList, paging)
            if (paging.ordering == SortOrder.ASC) {
                rows.sortedBy { it.svar.gjelderFra }.take(paging.size)
            } else {
                rows.sortedByDescending { it.svar.gjelderFra }.take(paging.size)
            }
        }

    fun lagreAlleBekreftelser(bekreftelser: Iterable<Bekreftelse>) {
        if (bekreftelser.iterator().hasNext()) {
            transaction {
                bekreftelser.forEach { bekreftelse ->
                    if (BekreftelseFunctions.getForBekreftelseId(bekreftelse.id) != null) {
                        logger.warn("Ignorerer mottatt bekreftelse som duplikat")
                    } else {
                        logger.debug("Lagrer ny bekreftelse")
                        BekreftelseFunctions.insert(bekreftelse)
                    }
                }
            }
        }
    }
}