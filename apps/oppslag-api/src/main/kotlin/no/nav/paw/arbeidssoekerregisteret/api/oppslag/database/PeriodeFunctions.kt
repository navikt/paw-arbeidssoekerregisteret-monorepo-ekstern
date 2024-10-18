package no.nav.paw.arbeidssoekerregisteret.api.oppslag.database

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.PeriodeRow
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.util.*

fun Transaction.finnPeriode(periodeId: UUID): PeriodeRow? {
    return PeriodeTable
        .join(StartetMetadataAlias, JoinType.LEFT, PeriodeTable.startetId, StartetMetadataAlias[MetadataTable.id])
        .join(AvsluttetMetadataAlias, JoinType.LEFT, PeriodeTable.avsluttetId, AvsluttetMetadataAlias[MetadataTable.id])
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

fun Transaction.finnPerioderForIdentiteter(identitetsnummerList: List<Identitetsnummer>): List<PeriodeRow> {
    val identer = identitetsnummerList.map { it.verdi }
    return PeriodeTable
        .join(StartetMetadataAlias, JoinType.LEFT, PeriodeTable.startetId, StartetMetadataAlias[MetadataTable.id])
        .join(AvsluttetMetadataAlias, JoinType.LEFT, PeriodeTable.avsluttetId, AvsluttetMetadataAlias[MetadataTable.id])
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
        .map { it.toPeriodeRow() }
}

fun Transaction.finnPerioder(periodeIdList: List<UUID>): List<PeriodeRow> {
    return PeriodeTable
        .join(StartetMetadataAlias, JoinType.LEFT, PeriodeTable.startetId, StartetMetadataAlias[MetadataTable.id])
        .join(AvsluttetMetadataAlias, JoinType.LEFT, PeriodeTable.avsluttetId, AvsluttetMetadataAlias[MetadataTable.id])
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

fun Transaction.opprettPeriode(periode: Periode) {
    PeriodeTable.insert {
        it[periodeId] = periode.id
        it[identitetsnummer] = periode.identitetsnummer
        it[startetId] = opprettMetadata(periode.startet)
        it[avsluttetId] = periode.avsluttet?.let { metadata -> opprettMetadata(metadata) }
    }
}

fun Transaction.oppdaterPeriode(periode: Periode, eksisterendePeriode: PeriodeRow) {
    oppdaterMetadata(periode.startet, eksisterendePeriode.startet)

    if (eksisterendePeriode.avsluttet != null) {
        if (periode.avsluttet == null) {
            PeriodeTable.update(where = { PeriodeTable.id eq eksisterendePeriode.id }) {
                it[avsluttetId] = null // Sletter referanse til avsluttet-metadata. Det vil føre til
            }
        } else {
            oppdaterMetadata(periode.avsluttet, eksisterendePeriode.avsluttet)
        }
    } else {
        if (periode.avsluttet != null) {
            PeriodeTable.update(where = { PeriodeTable.id eq eksisterendePeriode.id }) {
                it[avsluttetId] = opprettMetadata(periode.avsluttet)
            }
        }
    }
}

fun Transaction.finnPeriodeOpplysningerRows(periodeId: UUID) =
    PeriodeOpplysningerTable.selectAll()
        .where { PeriodeOpplysningerTable.periodeId eq periodeId }
        .map { it.toPeriodeOpplysningerRow() }

fun Transaction.finnPeriodeOpplysningerRows() =
    PeriodeOpplysningerTable.selectAll()
        .map { it.toPeriodeOpplysningerRow() }
