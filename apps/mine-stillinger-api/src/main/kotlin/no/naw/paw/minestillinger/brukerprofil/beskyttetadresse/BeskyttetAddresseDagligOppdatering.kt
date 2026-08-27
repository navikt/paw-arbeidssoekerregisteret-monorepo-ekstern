package no.naw.paw.minestillinger.brukerprofil.beskyttetadresse

import io.opentelemetry.api.common.AttributeKey.longKey
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import kotlinx.coroutines.delay
import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.health.LivenessCheck
import no.nav.paw.health.ReadinessCheck
import no.nav.paw.health.StartupCheck
import no.naw.paw.minestillinger.Clock
import no.naw.paw.minestillinger.appLogger
import no.naw.paw.minestillinger.brukerprofil.BrukerprofilTjeneste
import no.naw.paw.minestillinger.db.ops.AdressebeskyttelseCacheStatus
import no.naw.paw.minestillinger.db.ops.hentAdressebeskyttelseCacheStatus
import no.naw.paw.minestillinger.db.ops.hentAlleAktiveBrukereMedUtløptAdressebeskyttelseFlagg
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import java.io.Closeable
import java.time.Duration
import java.time.Duration.between
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
private val MAKS_ADRESSEBESKYTTELSE_CACHE_ALDER: Duration = Duration.ofHours(36)
private val MAKS_TID_UTEN_FREMGANG: Duration = Duration.ofHours(12)
private val MAKS_ALDER_CACHE_STATUS: Duration = Duration.ofMinutes(30)

class BeskyttetAddresseDagligOppdatering(
    private val pdlFunction: suspend (List<Identitetsnummer>) -> List<AdressebeskyttelseResultat>,
    private val adresseBeskyttelseGyldighetsperiode: Duration,
    private val clock: Clock,
    private val brukerprofilTjeneste: BrukerprofilTjeneste,
    private val interval: Duration = Duration.ofMinutes(15),
    private val pdlBulkSize: Int = 1000,
    private val hentCacheStatus: () -> AdressebeskyttelseCacheStatus = ::hentAdressebeskyttelseCacheStatus
) : LivenessCheck, ReadinessCheck, StartupCheck, Closeable {
    private val startet = AtomicBoolean(false)
    private val sisteKjøring = AtomicReference<Instant>(Instant.EPOCH)
    private val sisteFremgang = AtomicReference<Instant>(Instant.EPOCH)
    private val cacheStatus = AtomicReference<CacheStatusMedTidspunkt?>(null)
    private val kjører = AtomicBoolean(false)
    private val skalFortsette = AtomicBoolean(true)

    suspend fun start() {
        if (!startet.compareAndSet(false, true)) {
            throw IllegalStateException("Kan ikke starte beskyttet adresse oppdatering flere ganger")
        }
        kjører.set(true)
        sisteFremgang.set(clock.now())
        try {
            oppdaterCacheStatus()
            while (skalFortsette.get()) {
                if (between(sisteKjøring.get(), clock.now()) > interval) {
                    appLogger.info("Starter oppdatering av adressebeskyttelse for brukerprofiler")
                    val antall = suspendTransaction {
                        finnOgOppdater()
                    }.also {
                        sisteKjøring.set(clock.now())
                        sisteFremgang.set(clock.now())
                        oppdaterCacheStatus()
                    }
                    appLogger.info("Brukerprofil: oppdaterte adressebeskyttelse for $antall brukere")
                }
                delay(timeMillis = 1000)
            }
        } finally {
            kjører.set(false)
        }
        appLogger.info("Jobb for oppdatering av beskyttet adresse er stoppet")
    }

    @WithSpan("vedlikehold_finn_og_oppdater_profiler_med_utgått_adresse_beskyttelse")
    suspend fun finnOgOppdater(): Int {
        val tidspunkt = clock.now()
        val finnAlleEldreEnn = tidspunkt - adresseBeskyttelseGyldighetsperiode
        return hentAlleAktiveBrukereMedUtløptAdressebeskyttelseFlagg(finnAlleEldreEnn)
            .also { brukere ->
                appLogger.info(
                    "Fant {} brukere med adressebeskyttelseflagg eldre enn {}",
                    brukere.size,
                    finnAlleEldreEnn
                )
                brukere.chunked(pdlBulkSize).forEach { chunk ->
                    brukerprofilTjeneste.oppdaterAdresseGraderingBulk(
                        brukerprofiler = chunk,
                        tidspunkt = clock.now()
                    )
                    sisteFremgang.set(clock.now())
                    oppdaterCacheStatusHvisNødvendig()
                }
            }.count()
            .also { count ->
                Span.current().setAttribute(longKey("antall"), count)
            }
    }


    override fun isAlive(): Boolean {
        return kjører.get() && erFremgangNylig(sisteFremgang.get(), clock.now())
    }

    override fun isReady(): Boolean {
        if (!isAlive()) return false
        val status = cacheStatus.get() ?: return false
        if (!erCacheStatusNylig(status.kontrollert, clock.now())) return false
        return erAdressebeskyttelseCacheFersk(status.status, clock.now())
    }

    override fun hasStarted(): Boolean {
        return startet.get()
    }

    override fun close() {
        appLogger.info("Stopper oppdatering av beskyttet adresse...")
        skalFortsette.set(false)
    }

    private fun oppdaterCacheStatusHvisNødvendig() {
        val sistKontrollert = cacheStatus.get()?.kontrollert ?: Instant.EPOCH
        if (between(sistKontrollert, clock.now()) >= interval) {
            oppdaterCacheStatus()
        }
    }

    private fun oppdaterCacheStatus() {
        try {
            cacheStatus.set(
                CacheStatusMedTidspunkt(
                    status = hentCacheStatus(),
                    kontrollert = clock.now()
                )
            )
        } catch (error: ExposedSQLException) {
            appLogger.error("Kunne ikke kontrollere alder på adressebeskyttelsesdata", error)
        }
    }
}

private data class CacheStatusMedTidspunkt(
    val status: AdressebeskyttelseCacheStatus,
    val kontrollert: Instant
)

internal fun erAdressebeskyttelseCacheFersk(
    status: AdressebeskyttelseCacheStatus,
    nå: Instant
): Boolean {
    if (status.manglerFlagg) return false
    return status.eldsteOppdatering
        ?.let { between(it, nå) <= MAKS_ADRESSEBESKYTTELSE_CACHE_ALDER }
        ?: true
}

internal fun erFremgangNylig(sisteFremgang: Instant, nå: Instant): Boolean =
    between(sisteFremgang, nå) <= MAKS_TID_UTEN_FREMGANG

internal fun erCacheStatusNylig(sistKontrollert: Instant, nå: Instant): Boolean =
    between(sistKontrollert, nå) <= MAKS_ALDER_CACHE_STATUS
