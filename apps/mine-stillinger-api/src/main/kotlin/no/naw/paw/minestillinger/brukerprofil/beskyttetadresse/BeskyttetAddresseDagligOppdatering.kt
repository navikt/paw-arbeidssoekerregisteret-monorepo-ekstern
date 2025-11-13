package no.naw.paw.minestillinger.brukerprofil.beskyttetadresse

import io.opentelemetry.api.common.AttributeKey.longKey
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.delay
import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.health.LivenessCheck
import no.nav.paw.health.ReadinessCheck
import no.nav.paw.health.StartupCheck
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
import kotlin.coroutines.cancellation.CancellationException


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
    private val jobb = AtomicReference<Deferred<Unit>?>(null)

    suspend fun start() {
        if (!startet.compareAndSet(false, true)) {
            throw IllegalStateException("Kan ikke starte beskyttet adresse oppdatering flere ganger")
        }
        while (skalFortsette.get()) {
            if (between(sisteKjøring.get(), clock.now()) > interval) {
                appLogger.info("Starter oppdatering av adressebeskyttelse for brukerprofiler")
                val antall = suspendedTransactionAsync {
                    finnOgOppdater()
                }.await().also {
                    sisteKjøring.set(clock.now())
                }
                appLogger.info("Brukerprofil: oppdaterte adressebeskyttelse for $antall brukere")
            }
            delay(timeMillis = 1000)
        }
        appLogger.info("Jobb for oppdatering av beskyttet adresse er stoppet")
    }

    @WithSpan("vedlikehold_finn_og_oppdater_profiler_med_utgått_adresse_beskyttelse")
    private suspend fun finnOgOppdater(): Int {
        val tidspunkt = clock.now()
        val finnAlleEldreEnn = tidspunkt - adresseBeskyttelseGyldighetsperiode
        return hentAlleAktiveBrukereMedUtløptAdressebeskyttelseFlagg(finnAlleEldreEnn)
            .also { brukere ->
                brukerprofilTjeneste.oppdaterAdresseGraderingBulk(
                    brukerprofiler = brukere,
                    tidspunkt = clock.now()
                )
            }.count()
            .also { count ->
                Span.current().setAttribute(longKey("antall"), count)
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
        appLogger.info("Stopper oppdatering av beskyttet adresse...")
        skalFortsette.set(false)
        jobb.get()?.cancel(CancellationException("Stoppet oppdatering av beskyttet adresse"))
    }
}
