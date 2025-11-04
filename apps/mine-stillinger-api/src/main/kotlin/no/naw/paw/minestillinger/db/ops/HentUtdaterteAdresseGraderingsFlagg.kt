package no.naw.paw.minestillinger.db.ops

import no.naw.paw.minestillinger.db.BrukerFlaggTable
import no.naw.paw.minestillinger.brukerprofil.flagg.HarBeskyttetadresseFlagg
import no.naw.paw.minestillinger.brukerprofil.flagg.HarBeskyttetAdresseFlaggtype
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.Instant

fun hentUtdaterteAdressegraderingsFlagg(
    sattFør: Instant,
    maksAntall: Int
): List<HarBeskyttetadresseFlagg> {
    return BrukerFlaggTable.selectAll()
        .where {
            (BrukerFlaggTable.navn eq HarBeskyttetAdresseFlaggtype.type) and (BrukerFlaggTable.tidspunkt less sattFør)
        }.limit(maksAntall)
        .map { row ->
            require(row[BrukerFlaggTable.navn] == HarBeskyttetAdresseFlaggtype.type) {
                "Forventet kun å hente flagg med navn ${HarBeskyttetAdresseFlaggtype.type}, men fikk ${row[BrukerFlaggTable.navn]}"
            }
            HarBeskyttetadresseFlagg(
                verdi = row[BrukerFlaggTable.verdi],
                tidspunkt = row[BrukerFlaggTable.tidspunkt]
            )
        }
}