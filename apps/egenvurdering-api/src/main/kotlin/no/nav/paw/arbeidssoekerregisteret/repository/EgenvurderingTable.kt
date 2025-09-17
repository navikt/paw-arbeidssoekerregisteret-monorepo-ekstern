package no.nav.paw.arbeidssoekerregisteret.repository

import org.jetbrains.exposed.v1.core.Table

object EgenvurderingTable : Table("egenvurdering") {
    val id = uuid("id")
    val profileringId = uuid("profilering_id").references(ProfileringTable.id)
    val egenvurdering = varchar("egenvurdering", length = 30)

    override val primaryKey = PrimaryKey(id)
}
