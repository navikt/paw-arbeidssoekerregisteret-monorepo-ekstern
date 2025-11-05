package no.naw.paw.minestillinger.brukerprofil

import io.ktor.utils.io.core.Closeable
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import no.nav.paw.health.LivenessCheck
import no.nav.paw.health.ReadinessCheck
import no.nav.paw.health.StartupCheck
import no.naw.paw.minestillinger.Clock
import no.naw.paw.minestillinger.appLogger
import no.naw.paw.minestillinger.db.ops.slettHvorPeriodeAvsluttetFør
import java.time.Duration
import java.time.Duration.between
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class SlettUbrukteBrukerprofiler(
    val forsinkelseFørSletting: Duration = Duration.ofDays(30),
    val interval: Duration = Duration.ofMinutes(15),
    val clock: Clock
) : ReadinessCheck, LivenessCheck, StartupCheck, Closeable {
    private val skalFortsette = AtomicBoolean(true)
    private val sisteKjøring = AtomicReference(Instant.EPOCH)
    private val harStartet = AtomicBoolean(false)

    suspend fun start(): Deferred<Unit> {
        if (!harStartet.compareAndSet(false, true)) {
            return CompletableDeferred<Unit>().apply {
                completeExceptionally(
                    IllegalStateException("Kan ikke starte sletting av ubrukte brukerprofiler flere ganger")
                )
            }
        }

        return coroutineScope {
            async {
                while (skalFortsette.get()) {
                    val grense = clock.now() - forsinkelseFørSletting
                    appLogger.info("Sletter alle profiler med avsluttet periode før $grense")
                    val antallSlettet = slettHvorPeriodeAvsluttetFør(grense)
                    appLogger.info("Slettet $antallSlettet ubrukte brukerprofiler")
                    delay(timeMillis = interval.toMillis())
                }
            }
        }
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
        return skalFortsette.set(false)
    }
}