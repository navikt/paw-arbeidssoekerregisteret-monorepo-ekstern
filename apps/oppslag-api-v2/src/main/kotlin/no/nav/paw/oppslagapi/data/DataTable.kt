package no.nav.paw.oppslagapi.data

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object DataTable: Table("data") {
    val id = long("id").autoIncrement()
    val identitetsnummer = char("identitetsnummer", 11).nullable()
    val periodeId = uuid("periode_id")
    val timestamp = timestamp("timestamp")
    val type = varchar("type", 255)
    val data = jsonb("data")
    override val primaryKey = PrimaryKey(id)
}