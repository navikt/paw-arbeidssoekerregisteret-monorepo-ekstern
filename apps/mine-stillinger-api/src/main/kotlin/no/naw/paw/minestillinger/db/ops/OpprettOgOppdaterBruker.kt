package no.naw.paw.minestillinger.db.ops

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.naw.paw.minestillinger.db.BrukerTable
import no.naw.paw.minestillinger.domain.KanTilbysTjenesten
import no.naw.paw.minestillinger.domain.TjenesteStatus
import no.naw.paw.minestillinger.domain.toDbString
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.upsert
import java.time.Instant

fun opprettOgOppdaterBruker(periode: Periode) {
    val avsluttet = periode.avsluttet
    if (avsluttet != null) {
        BrukerTable.update(
            body = {
                it[BrukerTable.arbeidssoekerperiodeAvsluttet] = avsluttet.tidspunkt
            },
            where = {
                BrukerTable.identitetsnummer eq periode.identitetsnummer
            }
        )
        BrukerTable.update(
            body = {
                it[BrukerTable.arbeidssoekerperiodeAvsluttet] = avsluttet.tidspunkt
                it[BrukerTable.tjenestestatus] = TjenesteStatus.INAKTIV.name
            },
            where = {
                BrukerTable.identitetsnummer eq periode.identitetsnummer
                BrukerTable.tjenestestatus eq TjenesteStatus.AKTIV.name
            }
        )
    } else {
        BrukerTable.upsert(
            keys = arrayOf(BrukerTable.identitetsnummer),
            onUpdateExclude = listOf(
                BrukerTable.identitetsnummer,
                BrukerTable.harBruktTjenesten,
            ),
            body = {
                it[identitetsnummer] = periode.identitetsnummer
                it[harBruktTjenesten] = false
                it[tjenestestatus] = TjenesteStatus.INAKTIV.name
                it[kanTilbysTjenesten] = KanTilbysTjenesten.UKJENT.toDbString()
                it[kanTilbysTjenestenTimestamp] = Instant.now()
                it[arbeidssoekerperiodeId] = periode.id
                it[arbeidssoekerperiodeAvsluttet] = null
            }
        )
    }
}