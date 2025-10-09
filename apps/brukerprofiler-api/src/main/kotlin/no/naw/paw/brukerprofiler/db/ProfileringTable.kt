package no.naw.paw.brukerprofiler.db

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp

object ProfileringTable : Table("profilering") {
    val id = long("id").autoIncrement()
    val periodeId = uuid("periode_id")
    val profileringId = uuid("profilering_id")
    val profileringTidspunkt = timestamp("profilering_tidspunkt")
    val profileringResultat = varchar(name = "profilering_resultat", length = 255)

    override val primaryKey = PrimaryKey(id)
}