package no.nav.paw.ledigestillinger.test

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.TemporalAmount


class MutableClock private constructor(
    private var instant: Instant,
    private val zone: ZoneId
) : Clock() {

    override fun getZone(): ZoneId = zone

    override fun withZone(zone: ZoneId): Clock = MutableClock(instant, zone)

    override fun instant(): Instant = instant

    fun advance(amount: TemporalAmount) {
        instant = instant.plus(amount)
    }

    fun rewind(amount: TemporalAmount) {
        instant = instant.minus(amount)
    }

    companion object {
        fun systemUTC(): MutableClock = MutableClock(
            instant = Instant.now(),
            zone = ZoneOffset.UTC
        )

        fun systemDefaultZone(): MutableClock = MutableClock(
            instant = Instant.now(),
            zone = ZoneId.systemDefault()
        )
    }
}