package no.nav.paw.ledigestillinger.util

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import no.nav.pam.stilling.ext.avro.AdStatus
import java.lang.Double
import kotlin.Number
import kotlin.String

fun MeterRegistry.meldingerMottattGauge(
    antallMottatt: Number,
    antallLagret: Number
) {
    meldingerMottattGauge("mottatt", antallMottatt)
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
    amount: Number = Double.valueOf(1.0),
) {
    counter(
        "paw.stillinger.counter.meldinger_mottatt",
        Tags.of(
            Tag.of("status", status.name),
        )
    ).increment(amount.toDouble())
}

fun MeterRegistry.finnStillingerByUuidListeGauge(
    antallUuider: Number
) {
    finnStillingerGauge("uuidListe", antallUuider)
}

fun MeterRegistry.finnStillingerByEgenskaperGauge(
    antallSoekeord: Number,
    antallKategorier: Number,
    antallFylker: Number,
    antallKommuner: Number
) {
    finnStillingerGauge("soekeord", antallSoekeord)
    finnStillingerGauge("kategorier", antallKategorier)
    finnStillingerGauge("fylker", antallFylker)
    finnStillingerGauge("kommuner", antallKommuner)
}

fun MeterRegistry.finnStillingerGauge(
    type: String,
    antall: Number
) {
    gauge(
        "paw.stillinger.gauge.finn_stillinger",
        Tags.of(
            Tag.of("type", type),
        ),
        antall
    )
}