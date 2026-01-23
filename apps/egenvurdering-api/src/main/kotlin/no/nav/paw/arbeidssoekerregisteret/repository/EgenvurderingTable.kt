package no.nav.paw.arbeidssoekerregisteret.repository

import no.nav.paw.arbeidssoekerregisteret.model.ProfilertTil
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID

object EgenvurderingTable : Table("egenvurdering") {
    val id = javaUUID("id")
    val profileringId = javaUUID("profilering_id").references(ProfileringTable.id)
    val egenvurdering = enumerationByName<ProfilertTil>("egenvurdering", length = 30)

    override val primaryKey = PrimaryKey(id)
}
