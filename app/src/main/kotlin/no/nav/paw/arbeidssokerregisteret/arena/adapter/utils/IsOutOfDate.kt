package no.nav.paw.arbeidssokerregisteret.arena.adapter.utils

import no.nav.paw.arbeidssokerregisteret.arena.helpers.v3.TopicsJoin
import java.time.Duration
import java.time.Instant

fun TopicsJoin.isOutOfDate(
    currentTime: Instant,
    maxAgeOfClosedPeriode: Duration = Duration.ofHours(1),
    maxAgeOfDanglingInfo: Duration = Duration.ofHours(1)
): Boolean {
    val avsluttet = periode?.avsluttet?.tidspunkt
    return if (avsluttet == null) {
        val tidspunktForOpplysninger = opplysningerOmArbeidssoeker?.sendtInnAv?.tidspunkt
        if (tidspunktForOpplysninger == null) {
            false
        } else {
            Duration.between(tidspunktForOpplysninger, currentTime) > maxAgeOfDanglingInfo
        }
    } else {
        Duration.between(avsluttet, currentTime) > maxAgeOfClosedPeriode
    }
}
