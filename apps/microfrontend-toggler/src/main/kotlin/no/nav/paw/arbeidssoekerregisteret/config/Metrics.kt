package no.nav.paw.arbeidssoekerregisteret.config

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import no.nav.paw.arbeidssoekerregisteret.model.Toggle
import java.util.concurrent.atomic.AtomicLong

private const val METRIC_PREFIX = "paw_microfrontend_toggler"

fun MeterRegistry.tellAntallSendteToggles(toggle: Toggle) {
    counter(
        "${METRIC_PREFIX}_processed_actions",
        Tags.of(
            Tag.of("microfrontend_id", toggle.microfrontendId),
            Tag.of("toggle_action", toggle.action.value)
        )
    ).increment()
}

fun MeterRegistry.tellAntallLagredePerioderTotalt(antallReference: AtomicLong) {
    gauge(
        "${METRIC_PREFIX}_antall_lagrede_perioder_totalt",
        Tags.empty(),
        antallReference
    ) {
        antallReference.get().toDouble()
    }
}

fun MeterRegistry.tellAntallLagredeAktivePerioder(antallReference: AtomicLong) {
    gauge(
        "${METRIC_PREFIX}_antall_lagrede_altive_perioder",
        Tags.empty(),
        antallReference
    ) {
        antallReference.get().toDouble()
    }
}

fun MeterRegistry.tellAntallLagredeAvsluttedePerioder(antallReference: AtomicLong) {
    gauge(
        "${METRIC_PREFIX}_antall_lagrede_avsluttede_perioder",
        Tags.empty(),
        antallReference
    ) {
        antallReference.get().toDouble()
    }
}

fun MeterRegistry.tellAntallMottattePerioder() {
    counter(
        "${METRIC_PREFIX}_antall_mottatte_perioder",
    ).increment()
}

fun MeterRegistry.tellAntallMottatteSiste14aVedtak() {
    counter(
        "${METRIC_PREFIX}_antall_mottatte_siste_14a_vedtak",
    ).increment()
}

fun MeterRegistry.tellAntallMottatteBeriket14aVedtak() {
    counter(
        "${METRIC_PREFIX}_antall_mottatte_beriket_14a_vedtak",
    ).increment()
}
