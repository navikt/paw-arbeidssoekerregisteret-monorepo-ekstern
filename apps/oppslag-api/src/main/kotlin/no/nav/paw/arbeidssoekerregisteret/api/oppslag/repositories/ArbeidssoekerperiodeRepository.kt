package no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.PeriodeTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ArbeidssoekerperiodeResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.toArbeidssoekerperiodeResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.toPeriode
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.oppdaterPeriode
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.finnPeriode
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.finnPerioder
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.opprettPeriode
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class ArbeidssoekerperiodeRepository(private val database: Database) {

    fun hentArbeidssoekerperiode(periodeId: UUID): Periode? =
        transaction(database) {
            finnPeriode(periodeId)?.toPeriode()
        }

    fun hentArbeidssoekerperioder(identitetsnummer: Identitetsnummer): List<ArbeidssoekerperiodeResponse> =
        transaction(database) {
            finnPerioder(identitetsnummer)
                .map { it.toArbeidssoekerperiodeResponse() }
        }

    fun hentAntallAktivePerioder(): Long =
        transaction(database) {
            PeriodeTable.selectAll().where { PeriodeTable.avsluttetId eq null }.count()
        }

    fun lagreArbeidssoekerperiode(periode: Periode) {
        transaction(database) {
            val eksisterendePeriode = finnPeriode(periode.id)

            if (eksisterendePeriode != null) {
                oppdaterPeriode(periode, eksisterendePeriode)
            } else {
                opprettPeriode(periode)
            }
        }
    }

    fun lagreArbeidssoekerperioder(perioder: Sequence<Periode>) {
        if (perioder.iterator().hasNext()) {
            transaction(database) {
                maxAttempts = 2
                minRetryDelay = 20

                val periodeIdList = perioder.map { it.id }.toList()
                val eksisterendePerioder = finnPerioder(periodeIdList)
                val eksisterendePeriodeIdMap = eksisterendePerioder.associateBy { it.periodeId }

                perioder.forEach { periode ->
                    val eksisterendePeriode = eksisterendePeriodeIdMap[periode.id]
                    if (eksisterendePeriode != null) {
                        oppdaterPeriode(periode, eksisterendePeriode)
                    } else {
                        opprettPeriode(periode)
                    }
                }
            }
        }
    }
}