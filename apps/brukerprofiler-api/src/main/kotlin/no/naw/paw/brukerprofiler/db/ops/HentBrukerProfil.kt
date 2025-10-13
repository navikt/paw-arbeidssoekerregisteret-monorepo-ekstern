package no.naw.paw.brukerprofiler.db.ops

import no.nav.paw.model.Identitetsnummer
import no.nav.paw.model.asIdentitetsnummer
import no.naw.paw.brukerprofiler.db.BrukerTable
import no.naw.paw.brukerprofiler.domain.BrukerProfil
import no.naw.paw.brukerprofiler.domain.KanTilbysTjenesten
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun hentBrukerProfil(identitetsnummer: Identitetsnummer): BrukerProfil? =
    transaction {
        BrukerTable.selectAll().where {
            BrukerTable.identitetsnummer eq identitetsnummer.verdi
        }.map { row ->
            BrukerProfil(
                id = row[BrukerTable.id],
                identitetsnummer = row[BrukerTable.identitetsnummer].asIdentitetsnummer(),
                tjenestenErAktiv = row[BrukerTable.tjenestenErAktiv],
                harBruktTjenesten = row[BrukerTable.harBruktTjenesten],
                arbeidssoekerperiodeId = row[BrukerTable.arbeidssoekerperiodeId],
                kanTilbysTjenesten = KanTilbysTjenesten.valueOf(row[BrukerTable.kanTilbysTjenesten]),
                kanTilbysTjenestenTimestamp = row[BrukerTable.kanTilbysTjenestenTimestamp],
                arbeidssoekerperiodeAvsluttet = row[BrukerTable.arbeidssoekerperiodeAvsluttet]
            )
        }.firstOrNull()
    }
