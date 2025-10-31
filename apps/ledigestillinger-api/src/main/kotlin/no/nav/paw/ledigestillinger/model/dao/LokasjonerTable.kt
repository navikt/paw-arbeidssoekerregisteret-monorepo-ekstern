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

    fun selectRowsByParentId(
        parentId: Long
    ): List<LokasjonRow> = selectAll()
        .where { LokasjonerTable.parentId eq parentId }
        .map { it.asLokasjonRow() }

    fun insert(
        parentId: Long,
        rows: Iterable<LokasjonRow>
    ) = batchInsert(rows) { row ->
        this[LokasjonerTable.parentId] = parentId
        this[adresse] = row.adresse
        this[postkode] = row.postkode
        this[poststed] = row.poststed
        this[kommune] = row.kommune
        this[kommunekode] = row.kommunekode
        this[fylke] = row.fylke
        this[fylkeskode] = row.fylkeskode
        this[land] = row.land
    }
}
