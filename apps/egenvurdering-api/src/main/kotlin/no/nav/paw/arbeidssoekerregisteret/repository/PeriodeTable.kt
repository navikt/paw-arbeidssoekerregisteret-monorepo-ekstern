package no.nav.paw.arbeidssoekerregisteret.repository

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object PeriodeTable : Table("periode") {
    val id = uuid("id")
    val identitetsnummer = varchar("identitetsnummer", length = 30)
    val startet = timestamp("startet")
    val avsluttet = timestamp("avsluttet").nullable()

    override val primaryKey = PrimaryKey(id)
}