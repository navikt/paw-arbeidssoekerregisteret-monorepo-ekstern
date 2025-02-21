package no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.PeriodeFunctions
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.PeriodeTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Paging
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.PeriodeRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.TraceParent
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.buildLogger
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.initSpan
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.model.Identitetsnummer
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class PeriodeRepository {
    private val logger = buildLogger

    fun hentPeriodeForId(periodeId: UUID): PeriodeRow? =
        transaction {
            PeriodeFunctions.getForPeriodeId(periodeId)
        }

    fun finnPerioderForIdentiteter(
        identitetsnummerList: Collection<Identitetsnummer>,
        paging: Paging = Paging()
    ): List<PeriodeRow> =
        transaction {
            val rows = PeriodeFunctions.findForIdentitetsnummerList(identitetsnummerList, paging)
            if (paging.ordering == SortOrder.ASC) {
                rows.sortedBy { it.startet.tidspunkt }.take(paging.size)
            } else {
                rows.sortedByDescending { it.startet.tidspunkt }.take(paging.size)
            }
        }

    fun tellAntallAktivePerioder(): Long =
        transaction {
            PeriodeTable.selectAll().where { PeriodeTable.avsluttetId eq null }.count()
        }

    fun lagrePeriode(periode: Periode) {
        transaction {
            val eksisterendePeriode = PeriodeFunctions.getForPeriodeId(periode.id)
            if (eksisterendePeriode != null) {
                logger.info("Endrer eksisterende periode")
                PeriodeFunctions.update(periode, eksisterendePeriode)
            } else {
                logger.info("Lagrer ny periode")
                PeriodeFunctions.insert(periode)
            }
        }
    }

    fun lagrePerioder(perioder: Iterable<Pair<TraceParent?, Periode>>) {
        transaction {
            perioder.forEach { (traceparent, periode) ->
                initSpan(traceparent, "paw.kafka.consumer.periode", "periode process")
                    .linkWithReplacedSpan()
                    .use {
                        logger.info("Lagrer periode")
                        val eksisterendePeriode = PeriodeFunctions.getForPeriodeId(periode.id)
                        if (eksisterendePeriode != null) {
                            logger.info("Endrer eksisterende periode")
                            PeriodeFunctions.update(periode, eksisterendePeriode)
                        } else {
                            logger.info("Lagrer ny periode")
                            PeriodeFunctions.insert(periode)
                        }
                    }
            }
        }
    }
}
