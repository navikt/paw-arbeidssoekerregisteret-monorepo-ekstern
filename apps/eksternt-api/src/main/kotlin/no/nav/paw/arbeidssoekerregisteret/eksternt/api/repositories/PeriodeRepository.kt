package no.nav.paw.arbeidssoekerregisteret.eksternt.api.repositories

import no.nav.paw.arbeidssoekerregisteret.eksternt.api.database.PeriodeTable
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.models.PeriodeRow
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.models.asPeriodeRow
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils.buildLogger
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils.toLocalDateTime
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.sql.SQLException
import java.time.Instant
import java.time.LocalDate
import java.util.*

class PeriodeRepository(private val database: Database) {
    private val logger = buildLogger

    fun hentPeriode(periodeId: UUID): PeriodeRow? {
        logger.debug("Hent periode")
        return transaction(database) {
            PeriodeTable.selectAll()
                .where { PeriodeTable.periodeId eq periodeId }
                .map { it.asPeriodeRow() }
                .firstOrNull()
        }
    }

    fun finnPerioder(
        identitetsnummer: Identitetsnummer,
        fraStartetDato: LocalDate? = null
    ): List<PeriodeRow> {
        logger.debug("Finner perioder")
        return transaction(database) {
            PeriodeTable.selectAll()
                .where { PeriodeTable.identitetsnummer eq identitetsnummer.verdi }
                .filter {
                    val startetDateTime = it[PeriodeTable.startet].toLocalDateTime()
                    fraStartetDato == null || startetDateTime.toLocalDate() >= fraStartetDato
                }.map { it.asPeriodeRow() }
        }
    }

    fun hentAntallAktivePerioder(): Long =
        transaction(database) {
            PeriodeTable.selectAll()
                .where { PeriodeTable.avsluttet eq null }
                .count()
        }

    fun opprettPeriode(periode: PeriodeRow) {
        logger.debug("Oppretter periode")
        transaction(database) {
            PeriodeTable.insert {
                it[periodeId] = periode.periodeId
                it[identitetsnummer] = periode.identitetsnummer
                it[startet] = periode.startet
                it[avsluttet] = periode.avsluttet
            }
        }
    }

    fun oppdaterPeriode(periode: PeriodeRow) {
        logger.debug("Oppdaterer periode")
        transaction(database) {
            try {
                PeriodeTable.update({ PeriodeTable.periodeId eq periode.periodeId }) {
                    it[identitetsnummer] = periode.identitetsnummer
                    it[avsluttet] = periode.avsluttet
                }
            } catch (e: SQLException) {
                logger.error("Feil ved oppdatering av periode", e)
                throw e
            }
        }
    }

    fun lagreAllePerioder(perioder: Iterable<Periode>) {
        transaction(database) {
            maxAttempts = 2
            minRetryDelay = 20

            perioder
                .forEach { periode ->
                    val eksisterendePeriode = hentPeriode(periode.id)
                    if (eksisterendePeriode == null) {
                        opprettPeriode(periode.asPeriodeRow())
                    } else {
                        oppdaterPeriode(periode.asPeriodeRow())
                    }
                }
        }
    }

    fun slettMedStartetEldreEnn(maksAlder: Instant): Int {
        logger.info("Sletter perioder som ble avsluttet tidligere enn {}", maksAlder.toString())
        return transaction(database) {
            PeriodeTable.deleteWhere { avsluttet less maksAlder }
        }
    }
}
