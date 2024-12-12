package no.nav.paw.arbeidssoekerregisteret.api.oppslag.services

import io.micrometer.core.instrument.MeterRegistry
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.PeriodeRepository
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.antallAktivePerioderGauge
import java.util.concurrent.atomic.AtomicLong

class MetricsService(
    private val meterRegistry: MeterRegistry,
    private val periodeRepository: PeriodeRepository
) {
    private val antallAktivePerioderReference = AtomicLong()

    fun tellAntallAktivePerioder() {
        val antallAktivePerioder = periodeRepository.tellAntallAktivePerioder()
        antallAktivePerioderReference.set(antallAktivePerioder)
        meterRegistry.antallAktivePerioderGauge(antallAktivePerioderReference)
    }
}