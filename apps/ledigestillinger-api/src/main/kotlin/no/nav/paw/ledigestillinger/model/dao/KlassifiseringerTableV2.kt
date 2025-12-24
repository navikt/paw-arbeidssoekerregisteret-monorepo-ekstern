package no.nav.paw.ledigestillinger.model.dao

import no.naw.paw.ledigestillinger.model.KlassifiseringType
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.selectAll

object KlassifiseringerTableV2 : LongIdTable("klassifiseringer_v2") {
    val parentId = long("parent_id").references(StillingerTableV2.id)
    val type = enumerationByName<KlassifiseringType>("type", 20)
    val kode = varchar("kode", 255)
    val navn = varchar("navn", 255)

    fun selectRowsByParentId(
        parentId: Long
    ): List<KlassifiseringRow> = selectAll()
        .where { KlassifiseringerTableV2.parentId eq parentId }
        .map { it.asKlassifiseringRowV2() }

    fun insert(
        parentId: Long,
        rows: Iterable<KlassifiseringRow>
    ) = batchInsert(rows) { row ->
        this[KlassifiseringerTableV2.parentId] = parentId
        this[KlassifiseringerTableV2.type] = row.type
        this[kode] = row.kode
        this[navn] = row.navn
    }
}
