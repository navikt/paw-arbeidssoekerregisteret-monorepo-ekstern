package no.naw.paw.minestillinger.db.ops

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.naw.paw.minestillinger.db.BrukerTable
import no.naw.paw.minestillinger.domain.BrukerId
import no.naw.paw.minestillinger.domain.FlaggListe
import no.naw.paw.minestillinger.domain.HarGradertAdresse
import no.naw.paw.minestillinger.domain.HarGradertAdresseFlagg
import no.naw.paw.minestillinger.domain.KanTilbysTjenesten
import no.naw.paw.minestillinger.domain.TjenesteStatus
import no.naw.paw.minestillinger.domain.TjenestenErAktiv
import no.naw.paw.minestillinger.domain.ingenFlagg
import no.naw.paw.minestillinger.domain.toDbString
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.upsert
import java.time.Instant

fun opprettOgOppdaterBruker(periode: Periode) {
    val avsluttet = periode.avsluttet
    val brukerId = BrukerTable.upsert(
        keys = arrayOf(BrukerTable.identitetsnummer),
        onUpdateExclude = listOf(
            BrukerTable.identitetsnummer
        ),
        body = {
            it[identitetsnummer] = periode.identitetsnummer
            it[arbeidssoekerperiodeId] = periode.id
            it[arbeidssoekerperiodeAvsluttet] = avsluttet?.tidspunkt
        }
    )[BrukerTable.id].let { brukerId ->
        BrukerId(brukerId)
    }
    val gjeldeneFlagg = FlaggListe(lesFlaggFraDB(brukerId))
    val flagg = if (avsluttet == null) {
        gjeldeneFlagg
            .addOrIgnore(
                HarGradertAdresse(
                    verdi = false,
                    tidspunkt = Instant.EPOCH
                )
            )
    } else {
        gjeldeneFlagg
            .addOrUpdate(
                TjenestenErAktiv(
                    verdi = false,
                    tidspunkt = avsluttet.tidspunkt
                )
            )
    }
    skrivFlaggTilDB(brukerId, flagg)
}