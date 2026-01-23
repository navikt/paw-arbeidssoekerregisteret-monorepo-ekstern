package no.nav.paw.arbeidssoekerregisteret.repository

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestamp

object PeriodeTable : Table("periode") {
    val id = javaUUID("id")
    val identitetsnummer = varchar("identitetsnummer", length = 30)
    val startet = timestamp("startet")
    val avsluttet = timestamp("avsluttet").nullable()

    override val primaryKey = PrimaryKey(id)
}