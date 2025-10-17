package no.nav.paw.ledigestillinger.model.dao

import no.nav.paw.ledigestillinger.model.StillingStatus
import no.nav.paw.ledigestillinger.model.VisningGrad
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant
import java.util.*

object StillingerTable : LongIdTable("stillinger") {
    val uuid = uuid("uuid")
    val adnr = varchar("adnr", 50).nullable()
    val tittel = varchar("tittel", 255)
    val status = enumerationByName<StillingStatus>("status", 20)
    val visning = enumerationByName<VisningGrad>("visning", 20)
    val kilde = varchar("kilde", 255)
    val medium = varchar("medium", 255)
    val referanse = varchar("referanse", 255)
    val arbeidsgiverNavn = varchar("arbeidsgiver_navn", 255).nullable()
    val opprettetTimestamp = timestamp("opprettet_timestamp")
    val endretTimestamp = timestamp("endret_timestamp")
    val publisertTimestamp = timestamp("publisert_timestamp")
    val utloeperTimestamp = timestamp("utloeper_timestamp").nullable()
    val messageTimestamp = timestamp("message_timestamp")
    val insertTimestamp = timestamp("inserted_timestamp")
    val updatedTimestamp = timestamp("updated_timestamp").nullable()
}

fun StillingerTable.selectIdByUUID(
    uuid: UUID
): Long? = select(StillingerTable.id)
    .where { StillingerTable.uuid eq uuid }
    .map { it[StillingerTable.id].value }
    .singleOrNull()

fun StillingerTable.selectRowByUUID(
    uuid: UUID
): StillingRow? = selectAll()
    .where { StillingerTable.uuid eq uuid }
    .map { it.asStillingRow() }
    .singleOrNull()

fun StillingerTable.insert(
    row: StillingRow
): Long = insertAndGetId {
    it[this.uuid] = row.uuid
    it[this.adnr] = row.adnr
    it[this.tittel] = row.tittel
    it[this.status] = row.status
    it[this.visning] = row.visning
    it[this.kilde] = row.kilde
    it[this.medium] = row.medium
    it[this.referanse] = row.referanse
    it[this.arbeidsgiverNavn] = row.arbeidsgiverNavn
    it[this.opprettetTimestamp] = row.opprettetTimestamp
    it[this.endretTimestamp] = row.endretTimestamp
    it[this.publisertTimestamp] = row.publisertTimestamp
    it[this.utloeperTimestamp] = row.utloeperTimestamp
    it[this.messageTimestamp] = row.messageTimestamp
    it[this.insertTimestamp] = Instant.now()
}.value

fun StillingerTable.updateById(
    id: Long,
    row: StillingRow
): Int = update(
    where = { StillingerTable.id eq id }
) {
    it[this.adnr] = row.adnr
    it[this.tittel] = row.tittel
    it[this.status] = row.status
    it[this.visning] = row.visning
    it[this.kilde] = row.kilde
    it[this.medium] = row.medium
    it[this.referanse] = row.referanse
    it[this.arbeidsgiverNavn] = row.arbeidsgiverNavn
    it[this.opprettetTimestamp] = row.opprettetTimestamp
    it[this.endretTimestamp] = row.endretTimestamp
    it[this.publisertTimestamp] = row.publisertTimestamp
    it[this.utloeperTimestamp] = row.utloeperTimestamp
    it[this.messageTimestamp] = row.messageTimestamp
    it[this.updatedTimestamp] = Instant.now()
}
