package no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.repositories.PeriodeRepository
import java.util.*

fun MeterRegistry.antallAktivePerioder(antall: Number) {
    gauge(
        "paw_arbeidssoekerregisteret_eksternt_api_antall_aktive_perioder",
        Tags.empty(),
        antall
    )
}

class AktivePerioderGaugeScheduler(
    meterRegistry: PrometheusMeterRegistry,
    periodeRepository: PeriodeRepository
) {
    private val timer = Timer()
    private val task =
        object : TimerTask() {
            override fun run() {
                val antallAktivePerioder = periodeRepository.hentAntallAktivePerioder()
                meterRegistry.antallAktivePerioder(antallAktivePerioder)
            }
        }

    fun scheduleGetAktivePerioderTask() = timer.scheduleAtFixedRate(task, 0L, 1000 * 60 * 10)
}
