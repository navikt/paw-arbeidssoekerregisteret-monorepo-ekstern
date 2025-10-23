package no.naw.paw.minestillinger.brukerprofil

import no.naw.paw.minestillinger.domain.HarGradertAdresse
import java.time.Duration
import java.time.Instant

val GRADERT_ADRESSE_GYLDIGHETS_PERIODE: Duration = Duration.ofDays(1)

fun HarGradertAdresse?.erGyldig(
    tidspunkt: Instant,
    gyldighetsperiode: Duration,
): Boolean {
    if (this == null) return false
    return Duration.between(this.tidspunkt, tidspunkt) < gyldighetsperiode
}
