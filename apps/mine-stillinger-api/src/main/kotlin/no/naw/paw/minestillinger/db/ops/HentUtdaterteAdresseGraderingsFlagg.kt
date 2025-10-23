package no.naw.paw.minestillinger.db.ops

import no.naw.paw.minestillinger.db.BrukerFlaggTable
import no.naw.paw.minestillinger.brukerprofil.flagg.HarGradertAdresseFlagg
import no.naw.paw.minestillinger.brukerprofil.flagg.HarGradertAdresseFlaggtype
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.Instant

fun hentUtdaterteAdressegraderingsFlagg(
    sattFør: Instant,
    maksAntall: Int
): List<HarGradertAdresseFlagg> {
    return BrukerFlaggTable.selectAll()
        .where {
            (BrukerFlaggTable.navn eq HarGradertAdresseFlaggtype.type) and (BrukerFlaggTable.tidspunkt less sattFør)
        }.limit(maksAntall)
        .map { row ->
            require(row[BrukerFlaggTable.navn] == HarGradertAdresseFlaggtype.type) {
                "Forventet kun å hente flagg med navn ${HarGradertAdresseFlaggtype.type}, men fikk ${row[BrukerFlaggTable.navn]}"
            }
            HarGradertAdresseFlagg(
                verdi = row[BrukerFlaggTable.verdi],
                tidspunkt = row[BrukerFlaggTable.tidspunkt]
            )
        }
}