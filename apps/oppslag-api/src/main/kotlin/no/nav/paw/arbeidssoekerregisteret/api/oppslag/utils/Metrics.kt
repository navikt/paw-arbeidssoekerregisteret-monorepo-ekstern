package no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils

import io.micrometer.core.instrument.Tags
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.PeriodeRepository
import java.util.*
import java.util.concurrent.atomic.AtomicLong

private val antallAktivePerioderReference = AtomicLong()

fun gaugeAntallAktivePerioder(
    registry: PrometheusMeterRegistry,
    repository: PeriodeRepository
) {
    val antallAktivePerioder = repository.tellAntallAktivePerioder()
    antallAktivePerioderReference.set(antallAktivePerioder)
    registry.gauge(
        "paw_arbeidssoekerregisteret_api_oppslag_antall_aktive_perioder",
        Tags.empty(),
        antallAktivePerioderReference
    ) {
        antallAktivePerioderReference.get().toDouble()
    }
}

class ScheduleGetAktivePerioderGaugeService(
    registry: PrometheusMeterRegistry,
    periodeRepository: PeriodeRepository
) {
    private val timer = Timer()
    private val task =
        object : TimerTask() {
            override fun run() {
                gaugeAntallAktivePerioder(registry, periodeRepository)
            }
        }

    fun scheduleGetAktivePerioderTask() = timer.scheduleAtFixedRate(task, 0L, 1000 * 60 * 10)
}
