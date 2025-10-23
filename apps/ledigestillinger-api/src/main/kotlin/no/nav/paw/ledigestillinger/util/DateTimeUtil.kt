package no.nav.paw.ledigestillinger.util

import no.nav.paw.logging.logger.buildNamedLogger
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val logger = buildNamedLogger("model.mapper")
val humanDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
val localDateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
val localDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
val zonedDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ISO_ZONED_DATE_TIME

fun String.fromLocalDateTimeString(): Instant = localDateTimeFormatter
    .parse(this, LocalDateTime::from)
    .toInstant(ZoneOffset.UTC)

fun LocalDateTime.toLocalDateTimeString(): String = localDateTimeFormatter
    .format(this.truncatedTo(ChronoUnit.MILLIS))

fun String.fromUnformattedString(): LocalDate? {
    val mappers: List<Function0<LocalDate?>> = listOf(
        ::humanDateOrNull,
        ::localDateOrNull,
        ::localDateTimeOrNull,
        ::zonedDateTimeOrNull
    )
    return mappers.firstNotNullOfOrNull { it.invoke() }
}

private fun String.humanDateOrNull(): LocalDate? = runCatching {
    humanDateFormatter.parse(this, LocalDate::from)
}.onFailure { cause -> logger.trace("Kunne ikke parse $this til LocalDate", cause) }.getOrNull()

private fun String.localDateOrNull(): LocalDate? = runCatching {
    localDateFormatter.parse(this, LocalDate::from)
}.onFailure { cause -> logger.trace("Kunne ikke parse $this til LocalDate", cause) }.getOrNull()

private fun String.localDateTimeOrNull(): LocalDate? = runCatching {
    localDateTimeFormatter.parse(this, LocalDateTime::from).toLocalDate()
}.onFailure { cause -> logger.trace("Kunne ikke parse $this til LocalDate", cause) }.getOrNull()

private fun String.zonedDateTimeOrNull(): LocalDate? = runCatching {
    zonedDateTimeFormatter.parse(this, ZonedDateTime::from).toLocalDate()
}.onFailure { cause -> logger.trace("Kunne ikke parse $this til LocalDate", cause) }.getOrNull()
