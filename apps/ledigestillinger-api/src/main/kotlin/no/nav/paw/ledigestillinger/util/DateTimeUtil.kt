package no.nav.paw.ledigestillinger.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
fun String.fromIsoString(): Instant = dateTimeFormatter
    .parse(this, LocalDateTime::from)
    .toInstant(ZoneOffset.UTC)

fun LocalDateTime.toIsoString(): String = dateTimeFormatter
    .format(this.truncatedTo(ChronoUnit.MILLIS))