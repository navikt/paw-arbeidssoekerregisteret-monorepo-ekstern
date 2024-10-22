package no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.PeriodeFunctions
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.PeriodeTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.PeriodeRow
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class PeriodeRepository(private val database: Database) {

    fun hentPeriodeForId(periodeId: UUID): PeriodeRow? =
        transaction(database) {
            PeriodeFunctions.getForPeriodeId(periodeId)
        }

    fun finnPerioderForIdentiteter(identitetsnummerList: List<Identitetsnummer>): List<PeriodeRow> =
        transaction(database) {
            PeriodeFunctions.findForIdentitetsnummerList(identitetsnummerList)
        }

    fun tellAntallAktivePerioder(): Long =
        transaction(database) {
            PeriodeTable.selectAll().where { PeriodeTable.avsluttetId eq null }.count()
        }

    fun lagrePeriode(periode: Periode) {
        transaction(database) {
            val eksisterendePeriode = PeriodeFunctions.getForPeriodeId(periode.id)

            if (eksisterendePeriode != null) {
                PeriodeFunctions.update(periode, eksisterendePeriode)
            } else {
                PeriodeFunctions.insert(periode)
            }
        }
    }

    fun lagreAllePerioder(perioder: Sequence<Periode>) {
        if (perioder.iterator().hasNext()) {
            transaction(database) {
                maxAttempts = 2
                minRetryDelay = 20

                val periodeIdList = perioder.map { it.id }.toList()
                val eksisterendePerioder = PeriodeFunctions.finnForPeriodeIdList(periodeIdList)
                val eksisterendePeriodeIdMap = eksisterendePerioder.associateBy { it.periodeId }

                perioder.forEach { periode ->
                    val eksisterendePeriode = eksisterendePeriodeIdMap[periode.id]
                    if (eksisterendePeriode != null) {
                        PeriodeFunctions.update(periode, eksisterendePeriode)
                    } else {
                        PeriodeFunctions.insert(periode)
                    }
                }
            }
        }
    }
}
