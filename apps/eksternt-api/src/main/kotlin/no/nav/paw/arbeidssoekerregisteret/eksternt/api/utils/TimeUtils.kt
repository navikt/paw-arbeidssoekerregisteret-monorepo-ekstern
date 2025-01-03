package no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils

import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

fun Instant.toLocalDateTime(): LocalDateTime = LocalDateTime.ofInstant(this, ZoneId.systemDefault())

fun LocalDateTime.toInstant(): Instant = this.atZone(ZoneId.systemDefault()).toInstant()

object TimeUtils {
    // Maks lagring for data er inneværende år pluss 3 år
    fun maksAlderForData(): Instant = LocalDateTime.now()
        .withDayOfYear(1)
        .withHour(0)
        .withMinute(0)
        .withSecond(0)
        .withNano(0)
        .minusYears(3)
        .atZone(ZoneId.systemDefault())
        .toInstant()

    fun tidTilMidnatt(): Duration {
        val midnight = LocalDateTime.now()
            .withHour(0)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
            .plusDays(1)
        return Duration.between(LocalDateTime.now(), midnight)
    }
}
