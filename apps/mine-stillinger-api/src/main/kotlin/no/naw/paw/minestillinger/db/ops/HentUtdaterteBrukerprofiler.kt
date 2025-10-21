package no.naw.paw.minestillinger.db.ops

import no.naw.paw.minestillinger.db.BrukerTable
import no.naw.paw.minestillinger.domain.BrukerProfil
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.Instant

fun hentUtdaterteBrukerprofiler(
    kanTilbysTjenesteOppdatertFør: Instant,
    maksAntall: Int
): List<BrukerProfil> {
    return BrukerTable.selectAll()
        .where {
            BrukerTable.kanTilbysTjenestenTimestamp greater kanTilbysTjenesteOppdatertFør
        }
        .limit(maksAntall)
        .map(::brukerprofil)
}