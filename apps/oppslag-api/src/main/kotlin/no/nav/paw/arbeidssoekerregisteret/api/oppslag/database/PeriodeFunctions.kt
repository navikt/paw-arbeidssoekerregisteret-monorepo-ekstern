package no.nav.paw.arbeidssoekerregisteret.api.oppslag.database

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Paging
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.PeriodeRow
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.model.Identitetsnummer
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.util.*

object PeriodeFunctions {

    fun getForPeriodeId(periodeId: UUID): PeriodeRow? {
        return PeriodeTable
            .join(StartetMetadataAlias, JoinType.LEFT, PeriodeTable.startetId, StartetMetadataAlias[MetadataTable.id])
            .join(
                AvsluttetMetadataAlias,
                JoinType.LEFT,
                PeriodeTable.avsluttetId,
                AvsluttetMetadataAlias[MetadataTable.id]
            )
            .join(
                StartetBrukerAlias,
                JoinType.LEFT,
                StartetMetadataAlias[MetadataTable.utfoertAvId],
                StartetBrukerAlias[BrukerTable.id]
            )
            .join(
                AvsluttetBrukerAlias,
                JoinType.LEFT,
                AvsluttetMetadataAlias[MetadataTable.utfoertAvId],
                AvsluttetBrukerAlias[BrukerTable.id]
            )
            .join(
                StartetTidspunktAlias,
                JoinType.LEFT,
                StartetMetadataAlias[MetadataTable.tidspunktFraKildeId],
                StartetTidspunktAlias[TidspunktFraKildeTable.id]
            )
            .join(
                AvsluttetTidspunktAlias,
                JoinType.LEFT,
                AvsluttetMetadataAlias[MetadataTable.tidspunktFraKildeId],
                AvsluttetTidspunktAlias[TidspunktFraKildeTable.id]
            )
            .selectAll()
            .where { PeriodeTable.periodeId eq periodeId }.singleOrNull()?.toPeriodeRow()
    }

    fun findForIdentitetsnummerList(
        identitetsnummerList: Collection<Identitetsnummer>,
        paging: Paging = Paging()
    ): List<PeriodeRow> {
        val identer = identitetsnummerList.map { it.verdi }
        return PeriodeTable
            .join(StartetMetadataAlias, JoinType.LEFT, PeriodeTable.startetId, StartetMetadataAlias[MetadataTable.id])
            .join(
                AvsluttetMetadataAlias,
                JoinType.LEFT,
                PeriodeTable.avsluttetId,
                AvsluttetMetadataAlias[MetadataTable.id]
            )
            .join(
                StartetBrukerAlias,
                JoinType.LEFT,
                StartetMetadataAlias[MetadataTable.utfoertAvId],
                StartetBrukerAlias[BrukerTable.id]
            )
            .join(
                AvsluttetBrukerAlias,
                JoinType.LEFT,
                AvsluttetMetadataAlias[MetadataTable.utfoertAvId],
                AvsluttetBrukerAlias[BrukerTable.id]
            )
            .join(
                StartetTidspunktAlias,
                JoinType.LEFT,
                StartetMetadataAlias[MetadataTable.tidspunktFraKildeId],
                StartetTidspunktAlias[TidspunktFraKildeTable.id]
            )
            .join(
                AvsluttetTidspunktAlias,
                JoinType.LEFT,
                AvsluttetMetadataAlias[MetadataTable.tidspunktFraKildeId],
                AvsluttetTidspunktAlias[TidspunktFraKildeTable.id]
            )
            .selectAll()
            .where { PeriodeTable.identitetsnummer inList identer }
            //.orderBy(StartetMetadataAlias[MetadataTable.tidspunkt], paging.ordering)
            //.limit(paging.size).offset(paging.offset)
            .map { it.toPeriodeRow() }
    }

    fun finnForPeriodeIdList(periodeIdList: Collection<UUID>): List<PeriodeRow> {
        return PeriodeTable
            .join(StartetMetadataAlias, JoinType.LEFT, PeriodeTable.startetId, StartetMetadataAlias[MetadataTable.id])
            .join(
                AvsluttetMetadataAlias,
                JoinType.LEFT,
                PeriodeTable.avsluttetId,
                AvsluttetMetadataAlias[MetadataTable.id]
            )
            .join(
                StartetBrukerAlias,
                JoinType.LEFT,
                StartetMetadataAlias[MetadataTable.utfoertAvId],
                StartetBrukerAlias[BrukerTable.id]
            )
            .join(
                AvsluttetBrukerAlias,
                JoinType.LEFT,
                AvsluttetMetadataAlias[MetadataTable.utfoertAvId],
                AvsluttetBrukerAlias[BrukerTable.id]
            )
            .join(
                StartetTidspunktAlias,
                JoinType.LEFT,
                StartetMetadataAlias[MetadataTable.tidspunktFraKildeId],
                StartetTidspunktAlias[TidspunktFraKildeTable.id]
            )
            .join(
                AvsluttetTidspunktAlias,
                JoinType.LEFT,
                AvsluttetMetadataAlias[MetadataTable.tidspunktFraKildeId],
                AvsluttetTidspunktAlias[TidspunktFraKildeTable.id]
            )
            .selectAll()
            .where { PeriodeTable.periodeId inList periodeIdList }
            .map { it.toPeriodeRow() }
    }

    fun findPeriodeIdForIdentitetsnummerList(
        identitetsnummerList: Collection<Identitetsnummer>
    ): List<UUID> {
        val identer = identitetsnummerList.map { it.verdi }
        return PeriodeTable
            .select(PeriodeTable.periodeId)
            .where { PeriodeTable.identitetsnummer inList identer }
            .map { it[PeriodeTable.periodeId] }
    }

    fun insert(periode: Periode) {
        PeriodeTable.insert {
            it[periodeId] = periode.id
            it[identitetsnummer] = periode.identitetsnummer
            it[startetId] = MetadataFunctions.insert(periode.startet)
            it[avsluttetId] = periode.avsluttet?.let { metadata -> MetadataFunctions.insert(metadata) }
        }
    }

    fun update(periode: Periode, eksisterendePeriode: PeriodeRow) {
        MetadataFunctions.update(periode.startet, eksisterendePeriode.startet)

        if (eksisterendePeriode.avsluttet != null) {
            if (periode.avsluttet == null) {
                PeriodeTable.update(where = { PeriodeTable.id eq eksisterendePeriode.id }) {
                    it[avsluttetId] =
                        null // Sletter referanse til avsluttet-metadata. Det betyr at perioden er aktiv igjen!
                }
            } else {
                MetadataFunctions.update(periode.avsluttet, eksisterendePeriode.avsluttet)
            }
        } else {
            if (periode.avsluttet != null) {
                PeriodeTable.update(where = { PeriodeTable.id eq eksisterendePeriode.id }) {
                    it[avsluttetId] = MetadataFunctions.insert(periode.avsluttet)
                }
            }
        }
    }
}
