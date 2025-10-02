package no.nav.paw.arbeidssoekerregisteret.api.oppslag.database

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.MetadataRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.TidspunktFraKildeRow
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v1.TidspunktFraKilde
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.upsert

val StartetMetadataAlias = MetadataTable.alias("startet_metadata")
val AvsluttetMetadataAlias = MetadataTable.alias("avsluttet_metadata")
val StartetBrukerAlias = BrukerTable.alias("startet_bruker")
val AvsluttetBrukerAlias = BrukerTable.alias("avsluttet_bruker")
val StartetTidspunktAlias = TidspunktFraKildeTable.alias("startet_tidspunkt")
val AvsluttetTidspunktAlias = TidspunktFraKildeTable.alias("avsluttet_tidspunkt")

object MetadataFunctions {

    fun insert(metadata: Metadata): Long {
        return MetadataTable.insertAndGetId {
            it[utfoertAvId] = upsert(metadata.utfoertAv)
            it[tidspunkt] = metadata.tidspunkt
            it[kilde] = metadata.kilde
            it[aarsak] = metadata.aarsak
            it[tidspunktFraKildeId] =
                metadata.tidspunktFraKilde?.let { tidspunkt -> insert(tidspunkt) }
        }.value
    }

    fun update(metadata: Metadata, eksisterendeMetadata: MetadataRow) {
        MetadataTable.update(where = { MetadataTable.id eq eksisterendeMetadata.id }) {
            it[utfoertAvId] = upsert(metadata.utfoertAv)
            it[tidspunkt] = metadata.tidspunkt
            it[kilde] = metadata.kilde
            it[aarsak] = metadata.aarsak
            it[tidspunktFraKildeId] =
                insertOrUpdate(
                    metadata.tidspunktFraKilde,
                    eksisterendeMetadata.tidspunktFraKilde
                )
        }
    }

    private fun upsert(bruker: Bruker): Long {
        val result = BrukerTable.upsert(
            BrukerTable.type,
            BrukerTable.brukerId,
            where = { (BrukerTable.type eq bruker.type) and (BrukerTable.brukerId eq bruker.id) }
        ) {
            it[type] = bruker.type
            it[brukerId] = bruker.id
        }.resultedValues?.singleOrNull() ?: throw IllegalStateException("Upsert-operasjon returnerte ingen resultat")
        return result[BrukerTable.id].value
    }

    private fun insertOrUpdate(
        tidspunktFraKilde: TidspunktFraKilde?,
        eksisterendeTidspunktFraKilde: TidspunktFraKildeRow?
    ): Long? {
        return if (tidspunktFraKilde == null) {
            null
        } else {
            if (eksisterendeTidspunktFraKilde == null) {
                insert(tidspunktFraKilde)
            } else {
                update(tidspunktFraKilde, eksisterendeTidspunktFraKilde)
            }
        }
    }

    private fun insert(tidspunktFraKilde: TidspunktFraKilde): Long {
        return TidspunktFraKildeTable.insertAndGetId {
            it[tidspunkt] = tidspunktFraKilde.tidspunkt
            it[avviksType] = tidspunktFraKilde.avviksType
        }.value
    }

    private fun update(
        tidspunktFraKilde: TidspunktFraKilde,
        eksisterendeTidspunktFraKilde: TidspunktFraKildeRow
    ): Long {
        TidspunktFraKildeTable.update(where = { TidspunktFraKildeTable.id eq eksisterendeTidspunktFraKilde.id }) {
            it[tidspunkt] = tidspunktFraKilde.tidspunkt
            it[avviksType] = tidspunktFraKilde.avviksType
        }
        return eksisterendeTidspunktFraKilde.id
    }
}
