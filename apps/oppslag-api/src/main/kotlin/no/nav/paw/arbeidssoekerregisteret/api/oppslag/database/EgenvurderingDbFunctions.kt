package no.nav.paw.arbeidssoekerregisteret.api.oppslag.database

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.EgenvurderingRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Paging
import no.nav.paw.arbeidssokerregisteret.api.v2.Egenvurdering
import no.nav.paw.model.Identitetsnummer
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import java.util.*

object EgenvurderingDbFunctions {

    fun findForPeriodeIdList(
        periodeIdList: Collection<UUID>,
        paging: Paging = Paging()
    ): List<EgenvurderingRow> {
        return EgenvurderingTable
            .join(MetadataTable, JoinType.LEFT, EgenvurderingTable.sendtInnAvId, MetadataTable.id)
            .join(BrukerTable, JoinType.LEFT, MetadataTable.utfoertAvId, BrukerTable.id)
            .join(TidspunktFraKildeTable, JoinType.LEFT, MetadataTable.tidspunktFraKildeId, TidspunktFraKildeTable.id)
            .selectAll()
            .where { EgenvurderingTable.periodeId inList periodeIdList }
            //.orderBy(MetadataTable.tidspunkt, paging.ordering)
            //.limit(paging.size).offset(paging.offset)
            .map { it.toEgenvurderingRow() }
    }

    fun findForIdentitetsnummerList(
        identitetsnummerList: Collection<Identitetsnummer>,
        paging: Paging = Paging()
    ): List<EgenvurderingRow> {
        val identer = identitetsnummerList.map { it.verdi }
        return EgenvurderingTable
            .join(MetadataTable, JoinType.LEFT, EgenvurderingTable.sendtInnAvId, MetadataTable.id)
            .join(BrukerTable, JoinType.LEFT, MetadataTable.utfoertAvId, BrukerTable.id)
            .join(TidspunktFraKildeTable, JoinType.LEFT, MetadataTable.tidspunktFraKildeId, TidspunktFraKildeTable.id)
            .join(PeriodeTable, JoinType.LEFT, EgenvurderingTable.periodeId, PeriodeTable.periodeId)
            .selectAll()
            .where { PeriodeTable.identitetsnummer inList identer }
            //.orderBy(MetadataTable.tidspunkt, paging.ordering)
            //.limit(paging.size).offset(paging.offset)
            .map { it.toEgenvurderingRow() }
    }

    fun getForProfileringId(
        profileringId: UUID
    ): EgenvurderingRow? {
        return EgenvurderingTable
            .join(MetadataTable, JoinType.LEFT, EgenvurderingTable.sendtInnAvId, MetadataTable.id)
            .join(BrukerTable, JoinType.LEFT, MetadataTable.utfoertAvId, BrukerTable.id)
            .join(TidspunktFraKildeTable, JoinType.LEFT, MetadataTable.tidspunktFraKildeId, TidspunktFraKildeTable.id)
            .selectAll()
            .where { EgenvurderingTable.profileringId eq profileringId }
            .firstOrNull()
            ?.toEgenvurderingRow()
    }

    fun getForEgenvurderingId(
        egenvurderingId: UUID
    ): EgenvurderingRow? {
        return EgenvurderingTable
            .join(MetadataTable, JoinType.LEFT, EgenvurderingTable.sendtInnAvId, MetadataTable.id)
            .join(BrukerTable, JoinType.LEFT, MetadataTable.utfoertAvId, BrukerTable.id)
            .join(TidspunktFraKildeTable, JoinType.LEFT, MetadataTable.tidspunktFraKildeId, TidspunktFraKildeTable.id)
            .selectAll()
            .where { EgenvurderingTable.egenvurderingId eq egenvurderingId }
            .firstOrNull()
            ?.toEgenvurderingRow()
    }

    fun getForPeriodeIdAndProfileringId(
        periodeId: UUID,
        profileringId: UUID
    ): List<EgenvurderingRow> {
        return EgenvurderingTable
            .join(MetadataTable, JoinType.LEFT, EgenvurderingTable.sendtInnAvId, MetadataTable.id)
            .join(BrukerTable, JoinType.LEFT, MetadataTable.utfoertAvId, BrukerTable.id)
            .join(TidspunktFraKildeTable, JoinType.LEFT, MetadataTable.tidspunktFraKildeId, TidspunktFraKildeTable.id)
            .selectAll()
            .where { EgenvurderingTable.periodeId eq periodeId and (EgenvurderingTable.profileringId eq profileringId) }
            .map {
                it.toEgenvurderingRow()
            }
    }

    fun insert(egenvurdering: Egenvurdering) {
        EgenvurderingTable.insert {
            it[egenvurderingId] = egenvurdering.id
            it[periodeId] = egenvurdering.periodeId
            it[opplysningerOmArbeidssoekerId] = egenvurdering.opplysningerOmArbeidssokerId
            it[profileringId] = egenvurdering.profileringId
            it[sendtInnAvId] = MetadataFunctions.insert(egenvurdering.sendtInnAv)
            it[profilertTil] = egenvurdering.profilertTil
            it[EgenvurderingTable.egenvurdering] = egenvurdering.egenvurdering
        }
    }
}

