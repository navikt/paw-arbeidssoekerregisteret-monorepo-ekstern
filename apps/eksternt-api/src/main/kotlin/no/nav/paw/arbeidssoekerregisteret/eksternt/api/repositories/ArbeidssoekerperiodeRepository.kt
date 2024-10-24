package no.nav.paw.arbeidssoekerregisteret.eksternt.api.repositories

import no.nav.paw.arbeidssoekerregisteret.eksternt.api.database.PeriodeTable
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.models.Arbeidssoekerperiode
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.models.toArbeidssoekerperiode
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils.logger
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils.toInstant
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
import java.util.UUID

class ArbeidssoekerperiodeRepository(private val database: Database) {
    fun storeBatch(arbeidssoekerperioder: Sequence<Periode>) {
        transaction(database) {
            repetitionAttempts = 2
            minRepetitionDelay = 20

            arbeidssoekerperioder.forEach { periode ->
                if (finnesArbeidssoekerperiode(periode.id)) {
                    oppdaterArbeidssoekerperiode(periode.toArbeidssoekerperiode())
                } else {
                    opprettArbeidssoekerperiode(periode.toArbeidssoekerperiode())
                }
            }
        }
    }

    fun hentArbeidssoekerperioder(
        identitetsnummer: Identitetsnummer,
        fraStartetDato: LocalDate?
    ): List<Arbeidssoekerperiode?> =
        transaction(database) {
            PeriodeTable.selectAll().where { PeriodeTable.identitetsnummer eq identitetsnummer.verdi }.filter {
                val startetDateTime = it[PeriodeTable.startet].toLocalDateTime().toLocalDate()
                fraStartetDato == null || startetDateTime >= fraStartetDato
            }.map { resultRow ->
                val startet = resultRow[PeriodeTable.startet].toLocalDateTime()
                val avsluttet = resultRow[PeriodeTable.avsluttet]?.toLocalDateTime()

                Arbeidssoekerperiode(
                    Identitetsnummer(resultRow[PeriodeTable.identitetsnummer]),
                    resultRow[PeriodeTable.periodeId],
                    startet,
                    avsluttet
                )
            }
        }

    fun slettDataEldreEnnDatoFraDatabase(dato: Instant): Int {
        return transaction(database) {
            PeriodeTable.deleteWhere { avsluttet less dato }
        }
    }

    fun hentArbeidssoekerperiode(periodeId: UUID): Arbeidssoekerperiode? =
        transaction(database) {
            PeriodeTable.selectAll().where { PeriodeTable.periodeId eq periodeId }.map { resultRow ->
                val startet = resultRow[PeriodeTable.startet].toLocalDateTime()
                val avsluttet = resultRow[PeriodeTable.avsluttet]?.toLocalDateTime()

                Arbeidssoekerperiode(
                    Identitetsnummer(resultRow[PeriodeTable.identitetsnummer]),
                    resultRow[PeriodeTable.periodeId],
                    startet,
                    avsluttet
                )
            }.firstOrNull()
        }

    fun finnesArbeidssoekerperiode(periodeId: UUID): Boolean =
        transaction(database) {
            PeriodeTable.selectAll().where { PeriodeTable.periodeId eq periodeId }.singleOrNull() != null
        }

    fun opprettArbeidssoekerperiode(periode: Arbeidssoekerperiode) {
        transaction(database) {
            PeriodeTable.insert {
                it[periodeId] = periode.periodeId
                it[identitetsnummer] = periode.identitetsnummer.verdi
                it[startet] = periode.startet.toInstant()
                it[avsluttet] = periode.avsluttet?.toInstant()
            }
        }
    }

    fun oppdaterArbeidssoekerperiode(periode: Arbeidssoekerperiode) {
        transaction(database) {
            try {
                PeriodeTable.update({ PeriodeTable.periodeId eq periode.periodeId }) {
                    it[identitetsnummer] = periode.identitetsnummer.verdi
                    it[avsluttet] = periode.avsluttet?.toInstant()
                }
            } catch (e: SQLException) {
                logger.error("Feil ved oppdatering av periode", e)
                throw e
            }
        }
    }

    fun hentAntallAktivePerioder(): Long =
        transaction(database) {
            PeriodeTable.selectAll().where { PeriodeTable.avsluttet eq null }.count()
        }
}
