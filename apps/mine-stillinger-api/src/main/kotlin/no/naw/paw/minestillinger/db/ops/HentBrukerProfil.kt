package no.naw.paw.minestillinger.db.ops

import no.nav.paw.model.Identitetsnummer
import no.nav.paw.model.asIdentitetsnummer
import no.naw.paw.minestillinger.db.BrukerTable
import no.naw.paw.minestillinger.domain.BrukerProfil
import no.naw.paw.minestillinger.domain.KanTilbysTjenesten
import no.naw.paw.minestillinger.domain.TjenesteStatus
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun hentBrukerProfil(identitetsnummer: Identitetsnummer): BrukerProfil? =
    transaction {
        BrukerTable.selectAll().where {
            BrukerTable.identitetsnummer eq identitetsnummer.verdi
        }
            .map(::brukerprofil)
            .firstOrNull()
    }

fun brukerprofil(row: ResultRow): BrukerProfil =
    BrukerProfil(
        id = row[BrukerTable.id],
        identitetsnummer = row[BrukerTable.identitetsnummer].asIdentitetsnummer(),
        harBruktTjenesten = row[BrukerTable.harBruktTjenesten],
        arbeidssoekerperiodeId = row[BrukerTable.arbeidssoekerperiodeId],
        kanTilbysTjenesten = KanTilbysTjenesten.valueOf(row[BrukerTable.kanTilbysTjenesten]),
        kanTilbysTjenestenTimestamp = row[BrukerTable.kanTilbysTjenestenTimestamp],
        tjenestestatus = row[BrukerTable.tjenestestatus].let { TjenesteStatus.valueOf(it) },
        arbeidssoekerperiodeAvsluttet = row[BrukerTable.arbeidssoekerperiodeAvsluttet]
    )
