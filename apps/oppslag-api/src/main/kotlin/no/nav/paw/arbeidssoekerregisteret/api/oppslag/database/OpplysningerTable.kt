package no.nav.paw.arbeidssoekerregisteret.api.oppslag.database

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.PGEnum
import no.nav.paw.arbeidssokerregisteret.api.v1.Beskrivelse
import no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.java.javaUUID

object OpplysningerOmArbeidssoekerTable : LongIdTable("opplysninger_om_arbeidssoeker") {
    val opplysningerOmArbeidssoekerId = javaUUID("opplysninger_om_arbeidssoeker_id")
    val sendtInnAvId = long("sendt_inn_av_id").references(MetadataTable.utfoertAvId)
    val utdanningId = long("utdanning_id").references(UtdanningTable.id).nullable()
    val helseId = long("helse_id").references(HelseTable.id).nullable()
    val annetId = long("annet_id").references(AnnetTable.id).nullable()
}

object PeriodeOpplysningerTable : LongIdTable("periode_opplysninger") {
    val periodeId = javaUUID("periode_id")
    val opplysningerOmArbeidssoekerTableId =
        long("opplysninger_om_arbeidssoeker_id").references(
            OpplysningerOmArbeidssoekerTable.id
        )
}

object UtdanningTable : LongIdTable("utdanning") {
    val nus = varchar("nus", 255)
    val bestaatt =
        customEnumeration(
            name = "bestaatt",
            sql = "JaNeiVetIkke",
            fromDb = { value -> JaNeiVetIkke.valueOf(value as String) },
            toDb = { PGEnum("JaNeiVetIkke", it) }
        ).nullable()
    val godkjent =
        customEnumeration("godkjent", "JaNeiVetIkke", { value -> JaNeiVetIkke.valueOf(value as String) }, { PGEnum("JaNeiVetIkke", it) }).nullable()
}

object HelseTable : LongIdTable("helse") {
    val helsetilstandHindrerArbeid =
        customEnumeration(
            name = "helsetilstand_hindrer_arbeid",
            sql = "JaNeiVetIkke",
            fromDb = { value -> JaNeiVetIkke.valueOf(value as String) },
            toDb = {
                PGEnum("JaNeiVetIkke", it)
            }
        )
}

object AnnetTable : LongIdTable("annet") {
    val andreForholdHindrerArbeid =
        customEnumeration(
            name = "andre_forhold_hindrer_arbeid",
            sql = "JaNeiVetIkke",
            fromDb = { value -> JaNeiVetIkke.valueOf(value as String) },
            toDb = { PGEnum("JaNeiVetIkke", it) }
        ).nullable()
}

object BeskrivelseMedDetaljerTable : LongIdTable("beskrivelse_med_detaljer") {
    val opplysningerOmArbeidssoekerId =
        long("opplysninger_om_arbeidssoeker_id").references(
            OpplysningerOmArbeidssoekerTable.id
        )
}

object BeskrivelseTable : LongIdTable("beskrivelse") {
    val beskrivelse =
        customEnumeration(
            name = "beskrivelse",
            sql = "Beskrivelse",
            fromDb = { value -> Beskrivelse.valueOf(value as String) },
            toDb = { PGEnum("BeskrivelseEnum", it) }
        )
    val beskrivelseMedDetaljerId = long("beskrivelse_med_detaljer_id").references(BeskrivelseMedDetaljerTable.id)
}

object DetaljerTable : LongIdTable("detaljer") {
    val beskrivelseId = long("beskrivelse_id").references(BeskrivelseTable.id)
    val noekkel = varchar("noekkel", 50)
    val verdi = varchar("verdi", 255)
}
