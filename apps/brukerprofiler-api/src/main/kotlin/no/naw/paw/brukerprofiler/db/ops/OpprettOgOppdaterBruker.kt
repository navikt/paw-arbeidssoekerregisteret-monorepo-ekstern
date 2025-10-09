package no.naw.paw.brukerprofiler.db.ops

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.naw.paw.brukerprofiler.db.BrukerTable
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.update

fun opprettOgOppdaterBruker(periode: Periode) {
    val avsluttet = periode.avsluttet
    if (avsluttet != null) {
        BrukerTable.update {
            it[BrukerTable.arbeidssoekerperiodeAvsluttet] = avsluttet.tidspunkt
            it[BrukerTable.tjenestenErAktiv] = false
        }
    } else {
        BrukerTable.insertIgnore {
            it[identitetsnummer] = periode.identitetsnummer
            it[tjenestenErAktiv] = false
            it[harBruktTjenesten] = false
            it[arbeidssoekerperiodeId] = periode.id
            it[arbeidssoekerperiodeAvsluttet] = null
        }
    }
}