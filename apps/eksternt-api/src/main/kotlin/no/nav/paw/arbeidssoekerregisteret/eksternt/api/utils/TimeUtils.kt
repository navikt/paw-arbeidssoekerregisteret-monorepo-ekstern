package no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

fun Instant.toLocalDateTime(): LocalDateTime = LocalDateTime.ofInstant(this, ZoneId.systemDefault())

fun LocalDateTime.toInstant(): Instant = this.atZone(ZoneId.systemDefault()).toInstant()

fun getDeletionInterval(): Long = 1000L * 60 * 60 * 24 // 24 timer

fun getDelayUntilMidnight(): Long {
    val now = LocalDateTime.now()
    val midnight = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(0, 0, 0, 0))
    return Duration.between(now, midnight).toMillis()
}

object TimeUtils {
    private val now = LocalDateTime.now()

    private fun getStartOfYear(): LocalDateTime = LocalDateTime.of(LocalDate.of(now.year, 1, 1), LocalTime.of(0, 0, 0, 0))

    private fun getDurationFromNowToStartOfYear(): Duration = Duration.between(now, getStartOfYear())

    // Maks lagring for data er inneværende år pluss 3 år
    fun getMaxDateForDatabaseStorage(): LocalDateTime = now.minus(getDurationFromNowToStartOfYear()).minusYears(3)
}
