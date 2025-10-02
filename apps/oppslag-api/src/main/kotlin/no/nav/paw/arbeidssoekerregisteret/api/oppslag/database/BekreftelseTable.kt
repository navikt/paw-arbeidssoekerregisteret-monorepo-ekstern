package no.nav.paw.arbeidssoekerregisteret.api.oppslag.database

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.PGEnum
import no.nav.paw.bekreftelse.melding.v1.vo.Bekreftelsesloesning
import no.nav.paw.bekreftelse.melding.v1.vo.BrukerType
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.javatime.timestamp

object BekreftelseSvarTable : LongIdTable("bekreftelse_svar") {
    val sendtInnId = long("sendt_inn_id").references(MetadataTable.id)
    val gjelderFra = timestamp("gjelder_fra")
    val gjelderTil = timestamp("gjelder_til")
    val harJobbetIDennePerioden = bool("har_jobbet_i_denne_perioden")
    val vilFortsetteSomArbeidssoeker = bool("vil_fortsette_som_arbeidssoeker")
}

object BekreftelseTable : LongIdTable("bekreftelse") {
    val bekreftelseId = uuid("bekreftelse_id")
    val periodeId = uuid("periode_id")
    val bekreftelsesloesning = enumerationByName<Bekreftelsesloesning>("bekreftelsesloesning", 50)
    val svarId = long("svar_id").references(BekreftelseSvarTable.id)
}

object BekreftelseBrukerTable : LongIdTable("bruker") {
    val brukerId = varchar("bruker_id", 255)
    val type = customEnumeration(
        "type",
        "BrukerType",
        { value -> BrukerType.valueOf(value as String) },
        { PGEnum("BrukerType", it) })
}
