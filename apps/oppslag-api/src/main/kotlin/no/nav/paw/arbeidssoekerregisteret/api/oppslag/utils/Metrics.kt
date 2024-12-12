package no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import java.util.concurrent.atomic.AtomicLong

fun MeterRegistry.antallAktivePerioderGauge(antallAktivePerioder: AtomicLong) {
    gauge(
        "paw_arbeidssoekerregisteret_api_oppslag_antall_aktive_perioder",
        Tags.empty(),
        antallAktivePerioder
    ) {
        antallAktivePerioder.get().toDouble()
    }
}
