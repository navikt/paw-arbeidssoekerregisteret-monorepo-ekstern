package no.nav.paw.arbeidssoekerregisteret.eksternt.api.database

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

object PeriodeTable : LongIdTable("periode") {
    val periodeId = uuid("periode_id").uniqueIndex()
    val identitetsnummer = varchar("identitetsnummer", 11)
    val startet = timestamp("startet")
    val avsluttet = timestamp("avsluttet").nullable()
}
