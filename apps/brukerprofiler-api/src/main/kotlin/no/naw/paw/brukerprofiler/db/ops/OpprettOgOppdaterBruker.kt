package no.naw.paw.brukerprofiler.db.ops

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.naw.paw.brukerprofiler.db.BrukerTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.update

fun opprettOgOppdaterBruker(periode: Periode) {
    val avsluttet = periode.avsluttet
    if (avsluttet != null) {
        BrukerTable.update(
            body = {
                it[BrukerTable.arbeidssoekerperiodeAvsluttet] = avsluttet.tidspunkt
                it[BrukerTable.tjenestenErAktiv] = false
            },
            where = {
                BrukerTable.identitetsnummer eq periode.identitetsnummer
            }
        )
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