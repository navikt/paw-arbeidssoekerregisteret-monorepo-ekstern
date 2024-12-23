package no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import java.util.concurrent.atomic.AtomicLong

private const val METRIC_PREFIX = "paw_arbeidssoekerregisteret_api_oppslag"

fun MeterRegistry.antallAktivePerioderGauge(antallAktivePerioder: AtomicLong) {
    genericGauge(
        suffix = "antall_aktive_perioder",
        number = antallAktivePerioder,
        source = "database",
        target = "metrics",
        action = "fetched"
    )
}

fun MeterRegistry.genericCounter(
    suffix: String = "counter",
    number: Number = java.lang.Double.valueOf(1.0),
    source: String,
    target: String,
    action: String
) {
    counter(
        "${METRIC_PREFIX}_${suffix}",
        Tags.of(
            Tag.of("source", source),
            Tag.of("target", target),
            Tag.of("action", action)
        )
    ).increment(number.toDouble())
}

fun MeterRegistry.genericGauge(
    suffix: String = "gauge",
    number: Number,
    source: String,
    target: String,
    action: String
) {
    gauge(
        "${METRIC_PREFIX}_${suffix}",
        Tags.of(
            Tag.of("source", source),
            Tag.of("target", target),
            Tag.of("action", action)
        ),
        number
    )
}
