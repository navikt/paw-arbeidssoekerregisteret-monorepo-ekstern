package no.nav.paw.arbeidssoekerregisteret.repository

import no.nav.paw.arbeidssoekerregisteret.model.ProfilertTil
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestamp

object ProfileringTable : Table("profilering") {
    val id = javaUUID("id")
    val periodeId = javaUUID("periode_id")
    val profileringTidspunkt = timestamp("profilering_tidspunkt")
    val profilertTil = enumerationByName<ProfilertTil>("profilert_til", length = 30)

    override val primaryKey = PrimaryKey(id)
}