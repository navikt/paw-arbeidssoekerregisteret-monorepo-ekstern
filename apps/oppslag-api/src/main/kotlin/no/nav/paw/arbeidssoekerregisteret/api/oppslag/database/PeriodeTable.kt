package no.nav.paw.arbeidssoekerregisteret.api.oppslag.database

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.PGEnum
import no.nav.paw.arbeidssokerregisteret.api.v1.AvviksType
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

object BrukerTable : LongIdTable("bruker") {
    val brukerId = varchar("bruker_id", 255)
    val type = customEnumeration("type", "BrukerType", { value -> BrukerType.valueOf(value as String) }, { PGEnum("BrukerType", it) })
}

object TidspunktFraKildeTable : LongIdTable("tidspunkt_fra_kilde") {
    val tidspunkt = timestamp("tidspunkt")
    val avviksType = customEnumeration("avviks_type", "AvviksType", { value -> AvviksType.valueOf(value as String) }, { PGEnum("AvviksType", it) })
}

object MetadataTable : LongIdTable("metadata") {
    val utfoertAvId = long("utfoert_av_id").references(BrukerTable.id)
    val tidspunkt = timestamp("tidspunkt")
    val kilde = varchar("kilde", 255)
    val aarsak = varchar("aarsak", 255)
    val tidspunktFraKildeId = long("tidspunkt_fra_kilde_id").references(TidspunktFraKildeTable.id).nullable()
}

object PeriodeTable : LongIdTable("periode") {
    val periodeId = uuid("periode_id").uniqueIndex()
    val identitetsnummer = varchar("identitetsnummer", 11)
    val startetId = long("startet_id").references(MetadataTable.id)
    val avsluttetId = long("avsluttet_id").references(MetadataTable.id).nullable()
}
