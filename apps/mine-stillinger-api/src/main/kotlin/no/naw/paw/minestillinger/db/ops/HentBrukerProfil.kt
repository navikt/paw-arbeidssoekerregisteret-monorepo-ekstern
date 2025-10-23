package no.naw.paw.minestillinger.db.ops

import no.nav.paw.model.Identitetsnummer
import no.nav.paw.model.asIdentitetsnummer
import no.naw.paw.minestillinger.db.BrukerTable
import no.naw.paw.minestillinger.domain.BrukerId
import no.naw.paw.minestillinger.domain.BrukerProfilerUtenFlagg
import no.naw.paw.minestillinger.domain.PeriodeId
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun hentBrukerProfilUtenFlagg(identitetsnummer: Identitetsnummer): BrukerProfilerUtenFlagg? =
    transaction {
        BrukerTable.selectAll().where {
            BrukerTable.identitetsnummer eq identitetsnummer.verdi
        }
            .map(::brukerprofilUtenFlagg)
            .firstOrNull()
    }

fun brukerprofilUtenFlagg(row: ResultRow): BrukerProfilerUtenFlagg {
    return BrukerProfilerUtenFlagg(
        id = BrukerId(row[BrukerTable.id]),
        identitetsnummer = row[BrukerTable.identitetsnummer].asIdentitetsnummer(),
        arbeidssoekerperiodeId = PeriodeId(row[BrukerTable.arbeidssoekerperiodeId]),
        arbeidssoekerperiodeAvsluttet = row[BrukerTable.arbeidssoekerperiodeAvsluttet]
    )
}
