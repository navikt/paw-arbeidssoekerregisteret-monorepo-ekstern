package no.naw.paw.minestillinger.db.ops

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.naw.paw.minestillinger.db.BrukerTable
import no.naw.paw.minestillinger.domain.BrukerId
import no.naw.paw.minestillinger.brukerprofil.flagg.HarBeskyttetadresseFlagg
import no.naw.paw.minestillinger.brukerprofil.flagg.TjenestenErAktivFlagg
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
    val flagg = if (avsluttet == null) {
        HarBeskyttetadresseFlagg(
            verdi = false,
            tidspunkt = Instant.EPOCH
        )
    } else {
        TjenestenErAktivFlagg(
            verdi = false,
            tidspunkt = avsluttet.tidspunkt
        )
    }
    skrivFlaggTilDB(brukerId, listOf(flagg))
}