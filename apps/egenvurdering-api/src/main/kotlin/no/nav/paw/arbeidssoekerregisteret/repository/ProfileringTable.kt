package no.nav.paw.arbeidssoekerregisteret.repository

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object ProfileringTable : Table("profilering") {
    val id = uuid("id")
    val periodeId = uuid("periode_id")
    val profileringTidspunkt = timestamp("profilering_tidspunkt")
    val profilertTil = varchar("profilert_til", length = 30)

    override val primaryKey = PrimaryKey(id)
}