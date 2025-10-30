package no.nav.paw.ledigestillinger.model.dao

import no.nav.paw.ledigestillinger.api.models.KlassifiseringType
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.selectAll

object KlassifiseringerTable : LongIdTable("klassifiseringer") {
    val parentId = long("parent_id").references(StillingerTable.id)
    val type = enumerationByName<KlassifiseringType>("type", 20)
    val kode = varchar("kode", 255)
    val navn = varchar("navn", 255)
}

fun KlassifiseringerTable.selectRowsByParentId(
    parentId: Long
): List<KlassifiseringRow> = selectAll()
    .where { KlassifiseringerTable.parentId eq parentId }
    .map { it.asKlassifiseringRow() }

fun KlassifiseringerTable.insert(
    parentId: Long,
    rows: Iterable<KlassifiseringRow>
) = batchInsert(rows) { row ->
    this[KlassifiseringerTable.parentId] = parentId
    this[KlassifiseringerTable.type] = row.type
    this[KlassifiseringerTable.kode] = row.kode
    this[KlassifiseringerTable.navn] = row.navn
}
