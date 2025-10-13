package no.naw.paw.brukerprofiler.db.ops

import no.nav.paw.model.Identitetsnummer
import no.naw.paw.brukerprofiler.db.BrukerTable
import no.naw.paw.brukerprofiler.domain.KanTilbysTjenesten
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant

fun settKanTilbysTjenesten(
    identitetsnummer: Identitetsnummer,
    tidspunkt: Instant,
    kanTilbysTjenesten: KanTilbysTjenesten
): Boolean {
    return BrukerTable.update(
        body = {
            it[BrukerTable.kanTilbysTjenesten] = kanTilbysTjenesten.name
            it[kanTilbysTjenestenTimestamp] = tidspunkt
        },
        where = {
            BrukerTable.identitetsnummer eq identitetsnummer.verdi
        }
    ) == 1
}