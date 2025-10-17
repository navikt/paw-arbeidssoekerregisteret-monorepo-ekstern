package no.naw.paw.minestillinger.db.ops

import no.nav.paw.model.Identitetsnummer
import no.naw.paw.minestillinger.db.BrukerTable
import no.naw.paw.minestillinger.domain.KanTilbysTjenesten
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant

fun setKanTilbysTjenesten(
    identitetsnummer: Identitetsnummer,
    tidspunkt: Instant,
    kanTilbysTjenesten: KanTilbysTjenesten
): Boolean {
    return transaction {
        BrukerTable.update(
            body = {
                it[BrukerTable.kanTilbysTjenesten] = kanTilbysTjenesten.name
                it[kanTilbysTjenestenTimestamp] = tidspunkt
            },
            where = {
                BrukerTable.identitetsnummer eq identitetsnummer.verdi
            }
        ) == 1
    }
}