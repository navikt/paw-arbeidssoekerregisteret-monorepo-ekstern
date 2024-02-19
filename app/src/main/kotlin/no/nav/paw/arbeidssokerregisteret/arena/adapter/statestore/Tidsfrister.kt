package no.nav.paw.arbeidssokerregisteret.arena.adapter.statestore

import java.time.Duration
import java.time.Duration.*
import java.time.Instant

data class Tidsfrister(
    val antattLiveProsesering: Boolean,
    val tidsfristVentePaaPeriode: Duration,
    val tidsfristVentePaaOpplysninger: Duration,
    val tidsfristVentePaaProfilering: Duration,
)

fun kalkulerTidsfrister(wallClock: Instant, streamTime: Instant): Tidsfrister {
    val isReplay = between(streamTime, wallClock) > ofMinutes(10)
    val tidsfristVentePaaPeriode = if (isReplay) ofHours(5) else ofMinutes(10)
    val tidsfristVentePaaOpplysninger = if (isReplay) ofHours(5) else ofMinutes(10)
    val tidsfristVentePaaProfilering = if (isReplay) ofDays(5) else ofDays(2)
    return Tidsfrister(
        antattLiveProsesering = !isReplay,
        tidsfristVentePaaPeriode = tidsfristVentePaaPeriode,
        tidsfristVentePaaOpplysninger = tidsfristVentePaaOpplysninger,
        tidsfristVentePaaProfilering = tidsfristVentePaaProfilering
    )
}