package no.nav.paw.arbeidssoekerregisteret.eksternt.api.database

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestamp

object PeriodeTable : LongIdTable("periode") {
    val periodeId = javaUUID("periode_id").uniqueIndex()
    val identitetsnummer = varchar("identitetsnummer", 11)
    val startet = timestamp("startet")
    val avsluttet = timestamp("avsluttet").nullable()
}
