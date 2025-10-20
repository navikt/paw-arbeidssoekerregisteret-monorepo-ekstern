package no.naw.paw.minestillinger.db

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp

object BrukerTable : Table("bruker") {
    val id = long("id").autoIncrement()
    val identitetsnummer = varchar("identitetsnummer", 11)
    val tjenestestatus = varchar("tjenestestatus", 50)
    val harBruktTjenesten = bool("har_brukt_tjenesten")
    val kanTilbysTjenesten = varchar("kan_tilbys_tjenesten", 10)
    val kanTilbysTjenestenTimestamp = timestamp("kan_tilbys_tjenesten_timestamp")
    val arbeidssoekerperiodeId = uuid("arbeidssoekerperiode_id")
    val arbeidssoekerperiodeAvsluttet = timestamp("arbeidssoekerperiode_avsluttet").nullable()

    override val primaryKey = PrimaryKey(id)
}