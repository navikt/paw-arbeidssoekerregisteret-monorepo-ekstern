package no.naw.paw.minestillinger.db

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp

object BrukerTable : Table("bruker") {
    val id = long("id").autoIncrement()
    val identitetsnummer = varchar("identitetsnummer", 11)
    val arbeidssoekerperiodeId = uuid("arbeidssoekerperiode_id")
    val arbeidssoekerperiodeAvsluttet = timestamp("arbeidssoekerperiode_avsluttet").nullable()

    override val primaryKey = PrimaryKey(id)
}

object BrukerFlaggTable: Table("bruker_flagg") {
    val id = long("id").autoIncrement()
    val brukerId = long("bruker_id").references(BrukerTable.id)
    val navn = varchar("navn", 100)
    val verdi = bool("verdi")
    val tidspunkt = timestamp("tidspunkt")

    override val primaryKey = PrimaryKey(id)
}