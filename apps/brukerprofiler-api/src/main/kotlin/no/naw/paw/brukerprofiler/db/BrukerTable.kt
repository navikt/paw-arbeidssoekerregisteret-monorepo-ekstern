package no.naw.paw.brukerprofiler.db

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp

object BrukerTable : Table("periode") {
    val id = long("id").autoIncrement()
    val identitetsnummer = varchar("identitetsnummer", 11)
    val tjenestenErAktiv = bool("tjenesten_er_aktiv")
    val harBruktTjenesten = bool("har_brukt_tjenesten")
    val arbeidssoekerperiodeId = uuid("arbeidssoekerperiode_id")
    val arbeidssoekerperiodeAvsluttet = timestamp("arbeidssoekerperiode_avsluttet").nullable()

    override val primaryKey = PrimaryKey(id)
}