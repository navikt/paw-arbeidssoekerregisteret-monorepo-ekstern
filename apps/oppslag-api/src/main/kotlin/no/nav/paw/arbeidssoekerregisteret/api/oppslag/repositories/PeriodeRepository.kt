package no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.PeriodeFunctions
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.PeriodeTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Paging
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.PeriodeRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.buildLogger
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.security.authentication.model.Identitetsnummer
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
                PeriodeFunctions.update(periode, eksisterendePeriode)
            } else {
                PeriodeFunctions.insert(periode)
            }
        }
    }

    fun lagreAllePerioder(perioder: Iterable<Periode>) {
        if (perioder.iterator().hasNext()) {
            transaction {
                maxAttempts = 2
                minRetryDelay = 20

                val periodeIdList = perioder.map { it.id }.toList()
                val eksisterendePerioder = PeriodeFunctions.finnForPeriodeIdList(periodeIdList)
                val eksisterendePeriodeIdMap = eksisterendePerioder.associateBy { it.periodeId }

                perioder.forEach { periode ->
                    val eksisterendePeriode = eksisterendePeriodeIdMap[periode.id]
                    if (eksisterendePeriode != null) {
                        logger.debug("Endrer eksisterende periode")
                        PeriodeFunctions.update(periode, eksisterendePeriode)
                    } else {
                        logger.debug("Lagrer ny periode")
                        PeriodeFunctions.insert(periode)
                    }
                }
            }
        }
    }
}
