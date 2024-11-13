package no.nav.paw.arbeidssoekerregisteret.api.oppslag.database

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.BekreftelseRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Paging
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.bekreftelse.melding.v1.vo.Bruker
import no.nav.paw.bekreftelse.melding.v1.vo.Metadata
import no.nav.paw.bekreftelse.melding.v1.vo.Svar
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.upsert
import java.util.*

object BekreftelseFunctions {

    fun getForBekreftelseId(bekreftelseId: UUID): BekreftelseRow? {
        return BekreftelseTable
            .join(BekreftelseSvarTable, JoinType.LEFT, BekreftelseTable.svarId, BekreftelseSvarTable.id)
            .join(MetadataTable, JoinType.LEFT, BekreftelseSvarTable.sendtInnId, MetadataTable.id)
            .join(BrukerTable, JoinType.LEFT, MetadataTable.utfoertAvId, BrukerTable.id)
            .selectAll()
            .where { BekreftelseTable.bekreftelseId eq bekreftelseId }
            .singleOrNull()?.toBekreftelseRow()
    }

    fun findForPeriodeIdList(
        periodeIdList: List<UUID>,
        paging: Paging = Paging()
    ): List<BekreftelseRow> {
        return BekreftelseTable
            .join(BekreftelseSvarTable, JoinType.LEFT, BekreftelseTable.svarId, BekreftelseSvarTable.id)
            .join(MetadataTable, JoinType.LEFT, BekreftelseSvarTable.sendtInnId, MetadataTable.id)
            .join(BrukerTable, JoinType.LEFT, MetadataTable.utfoertAvId, BrukerTable.id)
            .selectAll()
            .where { BekreftelseTable.periodeId inList periodeIdList }
            //.orderBy(BekreftelseSvarTable.gjelderFra, paging.ordering)
            //.limit(paging.size).offset(paging.offset)
            .map { it.toBekreftelseRow() }
    }

    fun findForIdentitetsnummerList(
        identitetsnummerList: List<Identitetsnummer>,
        paging: Paging = Paging()
    ): List<BekreftelseRow> {
        val identiteter = identitetsnummerList.map { it.verdi }
        return BekreftelseTable
            .join(BekreftelseSvarTable, JoinType.LEFT, BekreftelseTable.svarId, BekreftelseSvarTable.id)
            .join(MetadataTable, JoinType.LEFT, BekreftelseSvarTable.sendtInnId, MetadataTable.id)
            .join(BrukerTable, JoinType.LEFT, MetadataTable.utfoertAvId, BrukerTable.id)
            .join(PeriodeTable, JoinType.LEFT, BekreftelseTable.periodeId, PeriodeTable.periodeId)
            .selectAll()
            .where { PeriodeTable.identitetsnummer inList identiteter }
            //.orderBy(BekreftelseSvarTable.gjelderFra, paging.ordering)
            //.limit(paging.size).offset(paging.offset)
            .map { it.toBekreftelseRow() }
    }

    fun insert(bekreftelse: Bekreftelse) {
        BekreftelseTable.insert {
            it[bekreftelseId] = bekreftelse.id
            it[periodeId] = bekreftelse.periodeId
            it[bekreftelsesloesning] = bekreftelse.bekreftelsesloesning
            it[svarId] = insert(bekreftelse.svar)
        }
    }

    private fun insert(svar: Svar) =
        BekreftelseSvarTable.insertAndGetId {
            it[sendtInnId] = insert(svar.sendtInnAv)
            it[gjelderFra] = svar.gjelderFra
            it[gjelderTil] = svar.gjelderTil
            it[harJobbetIDennePerioden] = svar.harJobbetIDennePerioden
            it[vilFortsetteSomArbeidssoeker] = svar.vilFortsetteSomArbeidssoeker
        }.value

    private fun insert(metadata: Metadata): Long =
        MetadataTable.insertAndGetId {
            it[utfoertAvId] = upsert(metadata.utfoertAv)
            it[tidspunkt] = metadata.tidspunkt
            it[kilde] = metadata.kilde
            it[aarsak] = metadata.aarsak
        }.value

    private fun upsert(bruker: Bruker): Long {
        val result = BekreftelseBrukerTable.upsert(
            BekreftelseBrukerTable.type,
            BekreftelseBrukerTable.brukerId,
            where = { (BekreftelseBrukerTable.type eq bruker.type) and (BekreftelseBrukerTable.brukerId eq bruker.id) }
        ) {
            it[type] = bruker.type
            it[brukerId] = bruker.id
        }.resultedValues?.singleOrNull() ?: throw IllegalStateException("Upsert-operasjon returnerte ingen resultat")
        return result[BekreftelseBrukerTable.id].value
    }

}