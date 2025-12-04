package no.nav.paw.ledigestillinger.util

import no.nav.pam.stilling.ext.avro.Ad
import no.nav.paw.hwm.Message
import no.naw.paw.ledigestillinger.model.Stilling
import java.time.Instant
import java.util.*

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
    val publishedTimestamp: Instant = published.fromLocalDateTimeString()
    return publishedTimestamp.isAfter(publisertGrense) && !erUtloept(utloperGrense)
}

fun Ad.erUtloept(
    utloperGrense: Instant
): Boolean {
    val expiresTimestamp: Instant? = expires?.fromLocalDateTimeString()
    return expiresTimestamp != null && expiresTimestamp.isBefore(utloperGrense)
}

fun Stilling.erUtloept(
    utloperGrense: Instant
): Boolean {
    return utloeper != null && utloeper!!.isBefore(utloperGrense)
}
