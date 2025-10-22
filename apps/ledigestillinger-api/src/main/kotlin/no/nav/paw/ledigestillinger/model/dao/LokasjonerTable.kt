package no.nav.paw.ledigestillinger.model.dao

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.selectAll

object LokasjonerTable : LongIdTable("lokasjoner") {
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

fun LokasjonerTable.selectRowsByParentId(
    parentId: Long
): List<LokasjonRow> = selectAll()
    .where { LokasjonerTable.parentId eq parentId }
    .map { it.asLokasjonRow() }

fun LokasjonerTable.insert(
    parentId: Long,
    rows: Iterable<LokasjonRow>
) = batchInsert(rows) { row ->
    this[LokasjonerTable.parentId] = parentId
    this[LokasjonerTable.adresse] = row.adresse
    this[LokasjonerTable.postkode] = row.postkode
    this[LokasjonerTable.poststed] = row.poststed
    this[LokasjonerTable.kommune] = row.kommune
    this[LokasjonerTable.kommunekode] = row.kommunekode
    this[LokasjonerTable.fylke] = row.fylke
    this[LokasjonerTable.fylkeskode] = row.fylkeskode
    this[LokasjonerTable.land] = row.land
}
