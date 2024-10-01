package no.nav.paw.arbeidssoekerregisteret.api.oppslag.metrics

import io.micrometer.core.instrument.Tags
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.ArbeidssoekerperiodeRepository
import java.util.*
import java.util.concurrent.atomic.AtomicLong

private val antallAktivePerioderReference = AtomicLong()

fun gaugeAntallAktivePerioder(
    registry: PrometheusMeterRegistry,
    repository: ArbeidssoekerperiodeRepository
) {
    val antallAktivePerioder = repository.hentAntallAktivePerioder()
    antallAktivePerioderReference.set(antallAktivePerioder)
    registry.gauge("paw_arbeidssoekerregisteret_api_oppslag_antall_aktive_perioder", Tags.empty(), antallAktivePerioderReference) {
        antallAktivePerioderReference.get().toDouble()
    }
}

class ScheduleGetAktivePerioderGaugeService(registry: PrometheusMeterRegistry, arbeidssoekerperiodeRepository: ArbeidssoekerperiodeRepository) {
    private val timer = Timer()
    private val task =
        object : TimerTask() {
            override fun run() {
                gaugeAntallAktivePerioder(registry, arbeidssoekerperiodeRepository)
            }
        }

    fun scheduleGetAktivePerioderTask() = timer.scheduleAtFixedRate(task, 0L, 1000 * 60 * 10)
}
