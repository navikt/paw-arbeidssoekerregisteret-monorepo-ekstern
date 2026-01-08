package no.nav.paw.ledigestillinger.model.dao

import no.nav.paw.ledigestillinger.model.asKlassifiseringRow
import no.naw.paw.ledigestillinger.model.KlassifiseringType
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.selectAll

object KlassifiseringerTable : LongIdTable("klassifiseringer_v2") {
    val parentId = long("parent_id").references(StillingerTable.id)
    val type = enumerationByName<KlassifiseringType>("type", 20)
    val kode = varchar("kode", 255)
    val navn = varchar("navn", 255)

    fun selectRowsByParentId(
        parentId: Long
    ): List<KlassifiseringRow> = selectAll()
        .where { KlassifiseringerTable.parentId eq parentId }
        .map { it.asKlassifiseringRow() }

    fun insert(
        parentId: Long,
        rows: Iterable<KlassifiseringRow>
    ) = batchInsert(rows) { row ->
        this[KlassifiseringerTable.parentId] = parentId
        this[KlassifiseringerTable.type] = row.type
        this[kode] = row.kode
        this[navn] = row.navn
    }
}
