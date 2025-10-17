package no.naw.paw.minestillinger.db.ops

import no.nav.paw.model.Identitetsnummer
import no.naw.paw.minestillinger.db.BrukerTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

fun setTjenestenErAktiv(
    identitetsnummer: Identitetsnummer,
    erTjenestenAktiv: Boolean,
): Boolean = transaction {
    BrukerTable.update(
        body = { it[tjenestenErAktiv] = erTjenestenAktiv },
        where = { BrukerTable.identitetsnummer eq identitetsnummer.verdi }
    ) == 1
}