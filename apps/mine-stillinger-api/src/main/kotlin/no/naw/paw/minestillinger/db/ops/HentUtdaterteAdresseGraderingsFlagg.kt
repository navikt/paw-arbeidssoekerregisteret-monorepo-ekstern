package no.naw.paw.minestillinger.db.ops

import no.naw.paw.minestillinger.db.BrukerFlaggTable
import no.naw.paw.minestillinger.db.BrukerTable
import no.naw.paw.minestillinger.domain.BrukerProfil
import no.naw.paw.minestillinger.domain.HarGradertAdresse
import no.naw.paw.minestillinger.domain.HarGradertAdresseFlagg
import no.naw.paw.minestillinger.domain.flaggNavn
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.Instant

fun hentUtdaterteAdressegraderingsFlagg(
    sattFør: Instant,
    maksAntall: Int
): List<HarGradertAdresse> {
    return BrukerFlaggTable.selectAll()
        .where {
            (BrukerFlaggTable.navn eq HarGradertAdresseFlagg.navn) and (BrukerFlaggTable.tidspunkt less sattFør)
        }.limit(maksAntall)
        .map { row ->
            require(row[BrukerFlaggTable.navn] == HarGradertAdresseFlagg.navn) {
                "Forventet kun å hente flagg med navn ${HarGradertAdresseFlagg.navn}, men fikk ${row[BrukerFlaggTable.navn]}"
            }
            HarGradertAdresse(
                verdi = row[BrukerFlaggTable.verdi],
                tidspunkt = row[BrukerFlaggTable.tidspunkt]
            )
        }
}