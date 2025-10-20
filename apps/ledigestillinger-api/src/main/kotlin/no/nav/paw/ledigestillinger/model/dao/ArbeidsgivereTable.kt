package no.nav.paw.ledigestillinger.model.dao

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

object ArbeidsgivereTable : LongIdTable("arbeidsgivere") {
    val parentId = long("parent_id").references(StillingerTable.id)
    val orgForm = varchar("org_form", 20)
    val orgNr = varchar("org_nr", 20)
    val parentOrgNr = varchar("parent_org_nr", 20)
    val navn = varchar("navn", 255)
    val offentligNavn = varchar("offentlig_navn", 255)
}

fun ArbeidsgivereTable.selectRowByParentId(
    parentId: Long
): ArbeidsgiverRow? = selectAll()
    .where { ArbeidsgivereTable.parentId eq parentId }
    .map { it.asArbeidsgiverRow() }
    .singleOrNull()

fun ArbeidsgivereTable.insert(
    parentId: Long,
    row: ArbeidsgiverRow
): Long = insertAndGetId {
    it[this.parentId] = parentId
    it[this.orgForm] = row.orgForm
    it[this.orgNr] = row.orgNr
    it[this.parentOrgNr] = row.parentOrgNr
    it[this.navn] = row.navn
    it[this.offentligNavn] = row.offentligNavn
}.value

fun ArbeidsgivereTable.updateByParentId(
    parentId: Long,
    row: ArbeidsgiverRow
): Int = update(
    where = { ArbeidsgivereTable.parentId eq parentId }
) {
    it[this.orgForm] = row.orgForm
    it[this.orgNr] = row.orgNr
    it[this.parentOrgNr] = row.parentOrgNr
    it[this.navn] = row.navn
    it[this.offentligNavn] = row.offentligNavn
}