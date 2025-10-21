package no.nav.paw.ledigestillinger.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

val humanDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
val localDateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
val localDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
val zonedDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ISO_ZONED_DATE_TIME

fun String.fromLocalDateTimeString(): Instant = localDateTimeFormatter
    .parse(this, LocalDateTime::from)
    .toInstant(ZoneOffset.UTC)

fun LocalDateTime.toLocalDateTimeString(): String = localDateTimeFormatter
    .format(this.truncatedTo(ChronoUnit.MILLIS))

fun String.fromUnformattedString(): Instant? = listOf(
    humanDateOrNull(),
    localDateOrNull(),
    localDateTimeOrNull(),
    zonedDateTimeOrNull()
).singleOrNull()

private fun String.humanDateOrNull(): Instant? = runCatching {
    humanDateFormatter
        .parse(this, LocalDate::from)
        .atStartOfDay()
        .toInstant(ZoneOffset.UTC)
}.getOrNull()

private fun String.localDateOrNull(): Instant? = runCatching {
    localDateFormatter
        .parse(this, LocalDate::from)
        .atStartOfDay()
        .toInstant(ZoneOffset.UTC)
}.getOrNull()

private fun String.localDateTimeOrNull(): Instant? = runCatching {
    localDateTimeFormatter
        .parse(this, LocalDateTime::from)
        .toInstant(ZoneOffset.UTC)
}.getOrNull()

private fun String.zonedDateTimeOrNull(): Instant? = runCatching {
    zonedDateTimeFormatter
        .parse(this, ZonedDateTime::from)
        .toInstant()
}.getOrNull()
