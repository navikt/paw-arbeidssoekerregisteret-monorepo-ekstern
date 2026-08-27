package no.naw.paw.minestillinger

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import no.nav.paw.health.LivenessCheck
import no.nav.paw.health.ReadinessCheck
import no.nav.paw.health.StartupCheck
import no.naw.paw.minestillinger.brukerprofil.BrukerprofilTjeneste
import no.naw.paw.minestillinger.brukerprofil.SlettGamlePropfileringerUtenProfil
import no.naw.paw.minestillinger.brukerprofil.SlettUbrukteBrukerprofiler
import no.naw.paw.minestillinger.brukerprofil.beskyttetadresse.ADRESSEBESKYTTELSE_GYLDIGHETS_PERIODE
import no.naw.paw.minestillinger.brukerprofil.beskyttetadresse.BeskyttetAddresseDagligOppdatering
import no.naw.paw.minestillinger.brukerprofil.beskyttetadresse.harBeskyttetAdresseBulk
import no.naw.paw.minestillinger.brukerprofil.direktemeldte.DirektemeldteStillingerFlaggOppdatering
import no.naw.paw.minestillinger.metrics.AntallBrukereMetrics
import java.io.Closeable
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random

class Bakgrunnsprosesser(
    val adresseBeskyttelseOppdatering: BeskyttetAddresseDagligOppdatering,
    val slettUbrukteBrukerprofiler: SlettUbrukteBrukerprofiler,
    val slettGamlePropfileringerUtenProfil: SlettGamlePropfileringerUtenProfil,
    val antallBrukereMetrics: AntallBrukereMetrics,
    val inkluderDirektemeldteStillingerFlaggOppdatering: DirektemeldteStillingerFlaggOppdatering,
    private val startupDelay: () -> Duration = ::randomStartupDelay,
    private val dispatcher: ExecutorCoroutineDispatcher = backgroundDispatcher()
) : StartupCheck, LivenessCheck, ReadinessCheck, Closeable {
    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(supervisorJob + dispatcher)
    private val startet = AtomicBoolean(false)
    private val workersStartet = AtomicBoolean(false)
    private val workerJobs = ConcurrentHashMap<String, Job>()

    fun start() {
        check(startet.compareAndSet(false, true)) { "Bakgrunnsprosesser kan kun startes én gang" }
        val delay = startupDelay()
        appLogger.info("Starter bakgrunnsjobber om ${delay.seconds} sekunder")
        scope.launch {
            delay(delay.toMillis())
            workers().forEach { worker ->
                workerJobs[worker.name] = scope.launch {
                    appLogger.info("Starter bakgrunnsjobb: ${worker.name}")
                    worker.start()
                }.apply {
                    invokeOnCompletion { throwable ->
                        when (throwable) {
                            null -> appLogger.info("Bakgrunnsjobb fullført: ${worker.name}")
                            is CancellationException -> appLogger.info("Bakgrunnsjobb stoppet: ${worker.name}")
                            else -> appLogger.error("Feil i bakgrunnsjobb: ${worker.name}", throwable)
                        }
                    }
                }
            }
            workersStartet.set(true)
            appLogger.info("Startet bakgrunnsjobber")
        }
    }

    override fun hasStarted(): Boolean = startet.get()

    override fun isAlive(): Boolean {
        if (!startet.get() || !supervisorJob.isActive) return false
        if (!workersStartet.get()) return true
        if (workerJobs.size != workers().size || workerJobs.values.any { !it.isActive }) return false
        return !adresseBeskyttelseOppdatering.hasStarted() || adresseBeskyttelseOppdatering.isAlive()
    }

    override fun isReady(): Boolean {
        return workersStartet.get() &&
            workerJobs.size == workers().size &&
            workerJobs.values.all(Job::isActive) &&
            adresseBeskyttelseOppdatering.isReady()
    }

    override fun close() {
        adresseBeskyttelseOppdatering.close()
        slettUbrukteBrukerprofiler.close()
        slettGamlePropfileringerUtenProfil.close()
        inkluderDirektemeldteStillingerFlaggOppdatering.close()
        scope.cancel()
        dispatcher.close()
    }

    private fun workers(): List<Worker> = listOf(
        Worker("slett_brukerprofiler", slettUbrukteBrukerprofiler::start),
        Worker("oppdater_adressebeskyttelse", adresseBeskyttelseOppdatering::start),
        Worker("oppdater_metrics", antallBrukereMetrics::startPeriodiskOppdateringAvMetrics),
        Worker("slette_frittstaende_profileringer", slettGamlePropfileringerUtenProfil::start),
        Worker(
            "oppdater_direktemeldte_stillinger_flagg",
            inkluderDirektemeldteStillingerFlaggOppdatering::start
        )
    )
}

private data class Worker(val name: String, val start: suspend () -> Unit)

private fun randomStartupDelay(): Duration = Duration.ofSeconds(Random.nextLong(5, 16))

private fun backgroundDispatcher(): ExecutorCoroutineDispatcher =
    Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "mine-stillinger-bakgrunnsjobber")
    }.asCoroutineDispatcher()

fun initBakgrunnsprosesser(
    webClients: WebClients,
    clock: Clock,
    brukerprofilTjeneste: BrukerprofilTjeneste,
    prometheusMeterRegistry: PrometheusMeterRegistry
): Bakgrunnsprosesser {
    val adresseBeskyttelseOppdatering = BeskyttetAddresseDagligOppdatering(
        pdlFunction = webClients.pdlClient::harBeskyttetAdresseBulk,
        adresseBeskyttelseGyldighetsperiode = ADRESSEBESKYTTELSE_GYLDIGHETS_PERIODE,
        clock = clock,
        brukerprofilTjeneste = brukerprofilTjeneste,
        interval = Duration.ofMinutes(15),
    )
    val slettUbrukteBrukerprofiler = SlettUbrukteBrukerprofiler(
        forsinkelseFørSletting = Duration.ofDays(30),
        interval = Duration.ofMinutes(17),
        clock = clock
    )
    val slettGamlePropfileringerUtenProfil = SlettGamlePropfileringerUtenProfil(
        forsinkelseFørSletting = Duration.ofDays(7),
        interval = Duration.ofMinutes(16),
        clock = clock
    )
    val inklusivDirektemeldteStillingerFlaggOppdatering = DirektemeldteStillingerFlaggOppdatering(
        direktemeldteStillingerTilgangClient = webClients.direktemeldteStillgerTilgangClient,
        clock = clock,
        oppdateringsintervall = Duration.ofMinutes(10),
        gyldighetsperiode = Duration.ofHours(4)
    )
    val antallBrukereMetrics = AntallBrukereMetrics(prometheusMeterRegistry)
    return Bakgrunnsprosesser(
        adresseBeskyttelseOppdatering = adresseBeskyttelseOppdatering,
        slettUbrukteBrukerprofiler = slettUbrukteBrukerprofiler,
        slettGamlePropfileringerUtenProfil = slettGamlePropfileringerUtenProfil,
        inkluderDirektemeldteStillingerFlaggOppdatering = inklusivDirektemeldteStillingerFlaggOppdatering,
        antallBrukereMetrics = antallBrukereMetrics
    )
}