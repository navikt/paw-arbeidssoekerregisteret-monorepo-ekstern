package no.naw.paw.brukerprofiler.db

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp

object SoekTable : Table("soek") {
    val id = long("id").autoIncrement()
    val opprettet = timestamp("opprettet")
    val brukerId = long("bruker_id").references(BrukerTable.id)
    val sistKjoert = timestamp("sist_kjoert").nullable()
    val type = varchar("type", 100)
    val soek = jsonb("soek")

    override val primaryKey = PrimaryKey(id)
}