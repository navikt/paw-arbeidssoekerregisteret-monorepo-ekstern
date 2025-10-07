package no.naw.paw.ledigestillinger.model.dao

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object ArbeidsgivereTable : LongIdTable("arbeidsgivere") {
    val parentId = long("parent_id").references(StillingerTable.id)
    val orgNr = varchar("org_nr", 20)
    val navn = varchar("navn", 255)
    val beskrivelse = varchar("beskrivelse", 255)
}