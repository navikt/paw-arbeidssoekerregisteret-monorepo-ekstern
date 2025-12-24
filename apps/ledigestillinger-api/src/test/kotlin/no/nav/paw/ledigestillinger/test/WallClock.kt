package no.nav.paw.ledigestillinger.test

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.TemporalAmount


class WallClock private constructor(
    private var instant: Instant,
    private val zone: ZoneId
) : Clock() {

    override fun getZone(): ZoneId = zone

    override fun withZone(zone: ZoneId): Clock = WallClock(instant, zone)

    override fun instant(): Instant = instant

    fun advance(amount: TemporalAmount) {
        instant = instant.plus(amount)
    }

    fun rewind(amount: TemporalAmount) {
        instant = instant.minus(amount)
    }

    companion object {
        fun systemUTC(): WallClock = WallClock(
            instant = Instant.now(),
            zone = ZoneOffset.UTC
        )

        fun systemDefaultZone(): WallClock = WallClock(
            instant = Instant.now(),
            zone = ZoneId.systemDefault()
        )
    }
}