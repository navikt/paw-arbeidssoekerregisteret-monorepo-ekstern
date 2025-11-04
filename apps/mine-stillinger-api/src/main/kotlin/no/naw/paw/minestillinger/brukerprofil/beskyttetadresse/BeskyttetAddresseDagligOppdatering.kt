package no.naw.paw.minestillinger.brukerprofil.beskyttetadresse

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import no.nav.paw.health.LivenessCheck
import no.nav.paw.health.ReadinessCheck
import no.nav.paw.health.StartupCheck
import no.nav.paw.model.Identitetsnummer
import no.naw.paw.minestillinger.Clock
import no.naw.paw.minestillinger.appLogger
import no.naw.paw.minestillinger.brukerprofil.BrukerprofilTjeneste
import no.naw.paw.minestillinger.db.ops.hentAlleAktiveBrukereMedUtløptAdressebeskyttelseFlagg
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.suspendedTransactionAsync
import java.io.Closeable
import java.time.Duration
import java.time.Duration.between
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference


class BeskyttetAddresseDagligOppdatering(
    private val pdlFunction: suspend (List<Identitetsnummer>) -> List<AdressebeskyttelseResultat>,
    private val adresseBeskyttelseGyldighetsperiode: Duration,
    private val clock: Clock,
    private val brukerprofilTjeneste: BrukerprofilTjeneste,
    private val interval: Duration = Duration.ofMinutes(15)
) : LivenessCheck, ReadinessCheck, StartupCheck, Closeable {
    private val startet = AtomicBoolean(false)
    private val sisteKjøring = AtomicReference<Instant>(Instant.EPOCH)
    private val skalFortsette = AtomicBoolean(true)

    suspend fun start(): Deferred<Unit> {
        if (!startet.compareAndSet(false, true)) {
            return CompletableDeferred<Unit>().apply {
                completeExceptionally(
                    IllegalStateException("Kan ikke starte beskyttet adresse oppdatering flere ganger")
                )
            }
        }
        return coroutineScope {
            async {
                while (skalFortsette.get()) {
                    appLogger.info("Starter oppdatering av adressebeskyttelse for brukerprofiler")
                    val antall = suspendedTransactionAsync {
                        val tidspunkt = clock.now()
                        val finnAlleEldreEnn = tidspunkt - adresseBeskyttelseGyldighetsperiode
                        hentAlleAktiveBrukereMedUtløptAdressebeskyttelseFlagg(finnAlleEldreEnn)
                            .map { profil ->
                                brukerprofilTjeneste.oppdaterAdresseGradering(profil, tidspunkt)
                            }.count()
                    }.await().also {
                        sisteKjøring.set(clock.now())
                    }
                    appLogger.info("Brukerprofil oppdatert adressebeskyttelse for $antall brukere")
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
        return startet.get()
    }

    override fun close() {
        return skalFortsette.set(false)
    }
}
