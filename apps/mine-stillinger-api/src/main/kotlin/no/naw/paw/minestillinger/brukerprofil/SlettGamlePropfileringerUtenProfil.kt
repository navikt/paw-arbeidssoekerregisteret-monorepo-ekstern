package no.naw.paw.minestillinger.brukerprofil

import io.ktor.utils.io.core.Closeable
import kotlinx.coroutines.delay
import no.nav.paw.health.LivenessCheck
import no.nav.paw.health.ReadinessCheck
import no.nav.paw.health.StartupCheck
import no.naw.paw.minestillinger.Clock
import no.naw.paw.minestillinger.Vedlikeholdslås
import no.naw.paw.minestillinger.appLogger
import no.naw.paw.minestillinger.db.ops.slettFrittståendeProfileringer
import no.naw.paw.minestillinger.db.ops.slettHvorPeriodeAvsluttetFør
import java.time.Duration
import java.time.Duration.between
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class SlettGamlePropfileringerUtenProfil(
    val forsinkelseFørSletting: Duration = Duration.ofDays(7),
    val interval: Duration = Duration.ofMinutes(15),
    val clock: Clock,
    private val vedlikeholdslås: Vedlikeholdslås = Vedlikeholdslås()
) : ReadinessCheck, LivenessCheck, StartupCheck, Closeable {
    private val skalFortsette = AtomicBoolean(true)
    private val sisteKjøring = AtomicReference(Instant.EPOCH)
    private val harStartet = AtomicBoolean(false)

    suspend fun start() {
        if (!harStartet.compareAndSet(false, true)) {
            throw IllegalStateException("Kan ikke starte sletting av frittstående profileringer flere ganger")
        }
        while (skalFortsette.get()) {
            if (between(sisteKjøring.get(), clock.now()) > interval) {
                vedlikeholdslås.kjørEksklusivt("slette_frittstaende_profileringer") {
                    val grense = clock.now() - forsinkelseFørSletting
                    slettFrittståendeProfileringer(grense)
                    sisteKjøring.set(clock.now())
                }
            }
            delay(timeMillis = 1000L)
        }
        appLogger.info("Jobb for sletting av frittstående profileringer er stoppet")
    }

    override fun isAlive(): Boolean {
        return between(sisteKjøring.get(), clock.now()) < (interval + Duration.ofMinutes(20))
    }

    override fun isReady(): Boolean {
        return between(sisteKjøring.get(), clock.now()) < (interval + Duration.ofMinutes(20))
    }

    override fun hasStarted(): Boolean {
        return harStartet.get()
    }

    override fun close() {
        appLogger.info("Stopper jobb for sletting av frittstående profileringer...")
        skalFortsette.set(false)
    }
}