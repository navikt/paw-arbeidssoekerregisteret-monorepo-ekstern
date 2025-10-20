package no.nav.paw.ledigestillinger.util

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import no.nav.pam.stilling.ext.avro.AdStatus

fun MeterRegistry.meldingerMottattGauge(
    antallTotal: Number,
    antallProsessert: Number,
    antallLagret: Number
) {
    meldingerMottattGauge("totalt", antallTotal)
    meldingerMottattGauge("prosessert", antallProsessert)
    meldingerMottattGauge("lagret", antallLagret)
}

fun MeterRegistry.meldingerMottattGauge(
    action: String,
    antall: Number
) {
    gauge(
        "paw.stillinger.gauge.meldinger_mottatt",
        Tags.of(
            Tag.of("action", action),
        ),
        antall
    )
}

fun MeterRegistry.meldingerMottattCounter(
    status: AdStatus,
    amount: Number = java.lang.Double.valueOf(1.0),
) {
    counter(
        "paw.stillinger.counter.meldinger_mottatt",
        Tags.of(
            Tag.of("status", status.name),
        )
    ).increment(amount.toDouble())
}