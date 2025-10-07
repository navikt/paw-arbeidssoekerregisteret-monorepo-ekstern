package no.naw.paw.ledigestillinger.model.dao

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object BeliggenheterTable : LongIdTable("beliggenheter") {
    val parentId = long("parent_id").references(StillingerTable.id)
    val adresse = varchar("adresse", 255).nullable()
    val postkode = varchar("postkode", 10).nullable()
    val poststed = varchar("poststed", 255).nullable()
    val kommune = varchar("kommune", 255).nullable()
    val fylke = varchar("fylke", 100).nullable()
    val land = varchar("land", 100)
}