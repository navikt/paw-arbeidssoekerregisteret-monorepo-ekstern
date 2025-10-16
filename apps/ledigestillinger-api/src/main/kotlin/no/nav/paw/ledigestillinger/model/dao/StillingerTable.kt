package no.nav.paw.ledigestillinger.model.dao

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.javatime.timestamp

object StillingerTable : LongIdTable("stillinger") {
    val uuid = uuid("uuid")
    val tittel = varchar("tittel", 255)
    val beskrivelse = varchar("beskrivelse", 255)
    val status = varchar("status", 30)
    val kilde = varchar("kilde", 255)
    val startDate = date("start_date")
    val annonseUrl = varchar("annonse_url", 255)
    val publisertTimestamp = timestamp("publisert_timestamp")
    val utloeperTimestamp = timestamp("utloeper_timestamp").nullable()
    val endretTimestamp = timestamp("endret_timestamp")
}