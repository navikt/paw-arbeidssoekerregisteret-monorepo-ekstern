package no.nav.paw.ledigestillinger.model.dao

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll

object ArbeidsgivereTableV2 : LongIdTable("arbeidsgivere_v2") {
    val parentId = long("parent_id").references(StillingerTableV2.id)
    val orgForm = varchar("org_form", 20)
    val orgNr = varchar("org_nr", 20).nullable()
    val parentOrgNr = varchar("parent_org_nr", 20).nullable()
    val navn = varchar("navn", 255)
    val offentligNavn = varchar("offentlig_navn", 255)

    fun selectRowByParentId(
        parentId: Long
    ): ArbeidsgiverRow? = selectAll()
        .where { ArbeidsgivereTableV2.parentId eq parentId }
        .map { it.asArbeidsgiverRowV2() }
        .singleOrNull()

    fun insert(
        parentId: Long,
        row: ArbeidsgiverRow
    ): Long = insertAndGetId {
        it[ArbeidsgivereTableV2.parentId] = parentId
        it[orgForm] = row.orgForm
        it[orgNr] = row.orgNr
        it[parentOrgNr] = row.parentOrgNr
        it[navn] = row.navn
        it[offentligNavn] = row.offentligNavn
    }.value
}
