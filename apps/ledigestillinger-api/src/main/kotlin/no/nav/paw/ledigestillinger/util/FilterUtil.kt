package no.nav.paw.ledigestillinger.util

import no.nav.pam.stilling.ext.avro.Ad
import no.nav.pam.stilling.ext.avro.AdStatus
import no.nav.paw.hwm.Message
import no.nav.paw.logging.logger.buildNamedLogger
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

private val logger = buildNamedLogger("message.filter")

fun Message<UUID, Ad>.skalBeholdes(
    publisertGrense: Instant,
    utloperGrense: Instant
): Boolean {
    return value.skalBeholdes(publisertGrense, utloperGrense)
}

fun Ad.skalBeholdes(
    publisertGrense: Instant,
    utloperGrense: Instant
): Boolean {
    return erPublisertEtter(publisertGrense) && !erIkkeAktivOgUtloept(utloperGrense)
}

private fun Ad.erPublisertEtter(
    publisertGrense: Instant
): Boolean {
    val publishedTimestamp: Instant = published.fromLocalDateTimeString()
    return publishedTimestamp.isAfter(publisertGrense).also { bool ->
        if (!bool) logger.info(
            "Filtert vekk stilling fordi den ble publisert {} som er før grensen {}",
            publishedTimestamp.truncatedTo(ChronoUnit.SECONDS),
            publisertGrense.truncatedTo(ChronoUnit.SECONDS)
        )
    }
}

private fun Ad.erIkkeAktivOgUtloept(
    utloperGrense: Instant
): Boolean {
    val expiresTimestamp: Instant? = expires?.fromLocalDateTimeString()
    return status != AdStatus.ACTIVE && expiresTimestamp != null && expiresTimestamp.isBefore(utloperGrense)
        .also { bool ->
            if (bool) logger.info(
                "Filtert vekk stilling fordi den har status {} og utløp {} som er før grensen {}",
                status.name,
                expiresTimestamp.truncatedTo(ChronoUnit.SECONDS),
                utloperGrense.truncatedTo(ChronoUnit.SECONDS)
            )
        }
}
