package no.nav.paw.arbeidssoekerregisteret.api.oppslag.database

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

object BekreftelseSvarTable : LongIdTable("bekreftelse_svar") {
    val sendtInnId = long("sendt_inn_id").references(MetadataTable.id)
    val gjelderFra = timestamp("gjelder_fra")
    val gjelderTil = timestamp("gjelder_til")
    val harJobbetIDennePerioden = bool("har_jobbet_i_denne_perioden")
    val vilFortsetteSomArbeidssoeker = bool("vil_fortsette_som_arbeidssoeker")
}

object BekreftelseTable : LongIdTable("bekreftelse") {
    val periodeId = uuid("periode_id")
    val namespace = varchar("namespace", 255)
    val svarId = long("svar_id").references(BekreftelseSvarTable.id)
}