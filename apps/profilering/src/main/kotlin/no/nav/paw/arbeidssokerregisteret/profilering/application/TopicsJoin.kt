package no.nav.paw.arbeidssokerregisteret.profilering.application

import no.nav.paw.arbeidssokerregisteret.api.helpers.v4.TopicsJoin
import java.time.Duration
import java.time.Instant
import java.util.*

fun TopicsJoin.isOutdated(
    currentTime: Instant,
    maxAgeForCompletedPeriode: Duration = Duration.ofHours(1),
    maxAgeForStandaloneOpplysning: Duration = Duration.ofHours(1)
): Boolean {
    return periode?.avsluttet?.tidspunkt?.let { completedEnd ->
        Duration.between(completedEnd, currentTime).abs() > maxAgeForCompletedPeriode
    } ?: (periode == null &&
            opplysningerOmArbeidssoeker?.sendtInnAv?.tidspunkt?.let { standaloneTimestamp ->
                Duration.between(standaloneTimestamp, currentTime).abs() > maxAgeForStandaloneOpplysning
            } ?: true)
}

fun TopicsJoin.periodeId(): UUID =
    periode?.id
        ?: opplysningerOmArbeidssoeker?.periodeId
        ?: profilering?.periodeId
        ?: throw IllegalStateException("Minst et felt i TopicsJoin må være satt!")

fun TopicsJoin.isComplete() = periode != null && opplysningerOmArbeidssoeker != null

infix fun TopicsJoin.mergeTo(existingData: TopicsJoin?): TopicsJoin =
    TopicsJoin(
        periode ?: existingData?.periode,
        profilering ?: existingData?.profilering,
        opplysningerOmArbeidssoeker ?: existingData?.opplysningerOmArbeidssoeker
    )