package no.naw.paw.minestillinger.db.ops

import no.naw.paw.minestillinger.db.BrukerFlaggTable
import no.naw.paw.minestillinger.domain.BrukerId
import no.naw.paw.minestillinger.brukerprofil.flagg.ListeMedFlagg
import no.naw.paw.minestillinger.brukerprofil.flagg.Flagg
import no.naw.paw.minestillinger.brukerprofil.flagg.LagretFlagg
import no.naw.paw.minestillinger.brukerprofil.flagg.flaggType
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.batchUpsert
import org.jetbrains.exposed.v1.jdbc.selectAll

fun skrivFlaggTilDB(brukerId: BrukerId, listeMedFlagg: Iterable<LagretFlagg>) {
    BrukerFlaggTable.batchUpsert(
        data = listeMedFlagg,
        keys = arrayOf(BrukerFlaggTable.brukerId, BrukerFlaggTable.navn),
        onUpdateExclude = listOf(BrukerFlaggTable.brukerId, BrukerFlaggTable.navn)
    ) { row ->
        this[BrukerFlaggTable.brukerId] = brukerId.verdi
        this[BrukerFlaggTable.navn] = row.type.type
        this[BrukerFlaggTable.verdi] = row.verdi
        this[BrukerFlaggTable.tidspunkt] = row.tidspunkt
    }
}

fun lesFlaggFraDB(brukerId: BrukerId): List<Flagg> {
    return BrukerFlaggTable.selectAll()
        .where { BrukerFlaggTable.brukerId eq brukerId.verdi }
        .map { row ->
            val lagretFlagNavn: String = row[BrukerFlaggTable.navn]
            flaggType(lagretFlagNavn)
                ?.flagg(verdi = row[BrukerFlaggTable.verdi], tidspunkt = row[BrukerFlaggTable.tidspunkt])
                ?: throw IllegalStateException("Ukjent flagg lagret i databasen: $lagretFlagNavn")

        }
}

