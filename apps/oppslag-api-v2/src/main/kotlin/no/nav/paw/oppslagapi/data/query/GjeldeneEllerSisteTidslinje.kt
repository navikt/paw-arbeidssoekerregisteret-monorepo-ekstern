package no.nav.paw.oppslagapi.data.query

import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Tidslinje as ApiTidslinje
import org.slf4j.LoggerFactory.getLogger
import java.time.Instant

private val tidslinjeFilterLogger = getLogger("tidslinje_filter")

fun List<ApiTidslinje>.gjeldeneEllerSisteTidslinje(): ApiTidslinje? {
    val aktive = this.filter { it.avsluttet == null }
    val antallAktive = aktive.size
    return when  {
        antallAktive == 1 -> aktive.first()
        antallAktive > 1 -> {
            tidslinjeFilterLogger.error("Flere aktive perioder funnet, bruker eldste som 'siste'")
            aktive.minBy { it.startet }
        }
        else -> maxByOrNull { it.avsluttet ?: Instant.MAX }
    }
}