package no.nav.paw.ledigestillinger.model.dao

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll

object BeliggenheterTable : LongIdTable("beliggenheter") {
    val parentId = long("parent_id").references(StillingerTable.id)
    val adresse = varchar("adresse", 255).nullable()
    val postkode = varchar("postkode", 10).nullable()
    val poststed = varchar("poststed", 100).nullable()
    val kommune = varchar("kommune", 100).nullable()
    val kommunekode = varchar("kommunekode", 20).nullable()
    val fylke = varchar("fylke", 100).nullable()
    val fylkeskode = varchar("fylkeskode", 20).nullable()
    val land = varchar("land", 100)
}

fun BeliggenheterTable.selectRowsByParentId(
    parentId: Long
): List<BeliggenhetRow> = selectAll()
    .where { BeliggenheterTable.parentId eq parentId }
    .map { it.asBeliggenhetRow() }

fun BeliggenheterTable.insert(
    parentId: Long,
    row: BeliggenhetRow
): Long = insertAndGetId {
    it[this.parentId] = parentId
    it[this.adresse] = row.adresse
    it[this.postkode] = row.postkode
    it[this.poststed] = row.poststed
    it[this.kommune] = row.kommune
    it[this.kommunekode] = row.kommunekode
    it[this.fylke] = row.fylke
    it[this.fylkeskode] = row.fylkeskode
    it[this.land] = row.land
}.value
