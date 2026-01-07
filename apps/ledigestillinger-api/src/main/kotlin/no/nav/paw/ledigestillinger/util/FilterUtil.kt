package no.nav.paw.ledigestillinger.util

import no.nav.pam.stilling.ext.avro.Ad
import no.nav.paw.hwm.Message
import no.nav.paw.logging.logger.buildNamedLogger
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

private val logger = buildNamedLogger("message.filter")

fun Message<UUID, Ad>.skalBeholdes(publisertGrense: Instant): Boolean {
    return value.erPublisertEtter(publisertGrense)
}

private fun Ad.erPublisertEtter(publisertGrense: Instant): Boolean {
    val publishedTimestamp: Instant = published.fromLocalDateTimeString()
    return publishedTimestamp.isAfter(publisertGrense).also { bool ->
        if (!bool) logger.info(
            "Filtert vekk stilling fordi den ble publisert {} som er før grensen {}",
            publishedTimestamp.truncatedTo(ChronoUnit.SECONDS),
            publisertGrense.truncatedTo(ChronoUnit.SECONDS)
        )
    }
}
