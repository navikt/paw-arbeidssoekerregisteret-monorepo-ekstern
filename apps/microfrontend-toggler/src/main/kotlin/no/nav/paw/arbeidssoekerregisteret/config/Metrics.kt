package no.nav.paw.arbeidssoekerregisteret.config

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import no.nav.paw.arbeidssoekerregisteret.model.Toggle
import no.nav.paw.arbeidssoekerregisteret.model.ToggleAction
import no.nav.paw.arbeidssoekerregisteret.model.ToggleSource
import java.util.concurrent.atomic.AtomicLong

private const val METRIC_PREFIX = "paw_microfrontend_toggler"

fun MeterRegistry.tellAntallSendteToggles(toggle: Toggle, source: ToggleSource, reason: String) {
    tellAntallSendteToggles(toggle.microfrontendId, source, toggle.action, reason)
}

fun MeterRegistry.tellAntallSendteToggles(
    target: String,
    source: ToggleSource,
    action: ToggleAction,
    reason: String
) {
    counter(
        "${METRIC_PREFIX}_antall_sendte_toggles",
        Tags.of(
            Tag.of("target", target),
            Tag.of("source", source.value),
            Tag.of("action", action.value),
            Tag.of("reason", reason)
        )
    ).increment()
}

fun MeterRegistry.tellAntallIkkeSendteToggles(
    target: String,
    source: ToggleSource,
    action: ToggleAction,
    reason: String
) {
    counter(
        "${METRIC_PREFIX}_antall_ikke_sendte_toggles",
        Tags.of(
            Tag.of("target", target),
            Tag.of("source", source.value),
            Tag.of("action", action.value),
            Tag.of("reason", reason)
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
        "${METRIC_PREFIX}_antall_lagrede_aktive_perioder",
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
