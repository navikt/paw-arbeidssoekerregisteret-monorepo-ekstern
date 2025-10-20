package no.naw.paw.minestillinger.db.ops

import no.nav.paw.model.Identitetsnummer
import no.naw.paw.minestillinger.db.BrukerTable
import no.naw.paw.minestillinger.domain.TjenesteStatus
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

fun setTjenestatus(
    identitetsnummer: Identitetsnummer,
    nyTjenestestatus: TjenesteStatus,
): Boolean = transaction {
    BrukerTable.update(
        body = { it[tjenestestatus] = nyTjenestestatus.name },
        where = { BrukerTable.identitetsnummer eq identitetsnummer.verdi }
    ) == 1
}