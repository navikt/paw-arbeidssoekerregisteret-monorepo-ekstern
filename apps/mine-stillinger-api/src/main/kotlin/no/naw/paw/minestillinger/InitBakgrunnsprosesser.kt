package no.naw.paw.minestillinger

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import no.nav.paw.health.HealthCheck
import no.nav.paw.health.model.HealthIndicator
import no.naw.paw.minestillinger.brukerprofil.BrukerprofilTjeneste
import no.naw.paw.minestillinger.brukerprofil.SlettGamlePropfileringerUtenProfil
import no.naw.paw.minestillinger.brukerprofil.SlettUbrukteBrukerprofiler
import no.naw.paw.minestillinger.brukerprofil.beskyttetadresse.ADRESSEBESKYTTELSE_GYLDIGHETS_PERIODE
import no.naw.paw.minestillinger.brukerprofil.beskyttetadresse.BeskyttetAddresseDagligOppdatering
import no.naw.paw.minestillinger.brukerprofil.beskyttetadresse.harBeskyttetAdresseBulk
import no.naw.paw.minestillinger.metrics.AntallBrukereMetrics
import java.time.Duration

class Bakgrunnsprosesser(
    val adresseBeskyttelseOppdatering: BeskyttetAddresseDagligOppdatering,
    val slettUbrukteBrukerprofiler: SlettUbrukteBrukerprofiler,
    val slettGamlePropfileringerUtenProfil: SlettGamlePropfileringerUtenProfil,
    val antallBrukereMetrics: AntallBrukereMetrics
) {
    fun helthChecks(): Iterable<HealthCheck> = listOf(
        adresseBeskyttelseOppdatering,
        slettUbrukteBrukerprofiler,
        slettGamlePropfileringerUtenProfil
    )
}

fun initBakgrunnsprosesser(
    webClients: WebClients,
    clock: SystemClock,
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
    val antallBrukereMetrics = AntallBrukereMetrics(prometheusMeterRegistry)
    appLogger.info("Starter bakgrunnsjobber...")
    GlobalScope.launch {
        val jobber = listOf(
            "slett_brukerprofiler" to async { slettUbrukteBrukerprofiler.start() },
            "oppdater_adressebeskyttelse" to async { adresseBeskyttelseOppdatering.start() },
            "oppdater_metrics" to async { antallBrukereMetrics.startPeriodiskOppdateringAvMetrics() },
            "slette_frittstaende_profileringer" to async { slettGamlePropfileringerUtenProfil.start() }
        )
        jobber.onEach { (beskrivelse, jobb) ->
            jobb.invokeOnCompletion { throwable ->
                if (throwable != null) {
                    appLogger.error("Feil i bakgrunnsjobb: $beskrivelse", throwable)
                } else {
                    appLogger.info("Bakgrunnsjobb fullført uten feil: $beskrivelse")
                }
            }
        }.forEach { (_, jobb) -> jobb.await() }
        appLogger.info("Alle jobber fullført")
    }
    appLogger.info("Startet bakgrunnsjobber")
    return Bakgrunnsprosesser(
        adresseBeskyttelseOppdatering = adresseBeskyttelseOppdatering,
        slettUbrukteBrukerprofiler = slettUbrukteBrukerprofiler,
        slettGamlePropfileringerUtenProfil = slettGamlePropfileringerUtenProfil,
        antallBrukereMetrics = antallBrukereMetrics
    )
}