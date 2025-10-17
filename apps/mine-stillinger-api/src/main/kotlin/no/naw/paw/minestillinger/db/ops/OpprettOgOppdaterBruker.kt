package no.naw.paw.minestillinger.db.ops

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.naw.paw.minestillinger.db.BrukerTable
import no.naw.paw.minestillinger.domain.KanTilbysTjenesten
import no.naw.paw.minestillinger.domain.toDbString
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant

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
            it[erIkkeInteressert] = false
            it[kanTilbysTjenesten] = KanTilbysTjenesten.UKJENT.toDbString()
            it[kanTilbysTjenestenTimestamp] = Instant.now()
            it[arbeidssoekerperiodeId] = periode.id
            it[arbeidssoekerperiodeAvsluttet] = null
        }
    }
}