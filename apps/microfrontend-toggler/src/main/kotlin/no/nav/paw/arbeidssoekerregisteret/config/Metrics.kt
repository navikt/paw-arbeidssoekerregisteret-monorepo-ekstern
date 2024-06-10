package no.nav.paw.arbeidssoekerregisteret.config

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import no.nav.paw.arbeidssoekerregisteret.model.Toggle
import java.util.concurrent.atomic.AtomicLong

private const val METRIC_PREFIX = "paw_microfrontend_toggler"

fun MeterRegistry.tellAntallToggles(toggle: Toggle) {
    counter(
        "${METRIC_PREFIX}_processed_actions",
        Tags.of(
            Tag.of("microfrontend_id", toggle.microfrontendId),
            Tag.of("toggle_action", toggle.action.value)
        )
    ).increment()
}

fun MeterRegistry.tellAntallLagredePerioder(antallLagredePerioderReference: AtomicLong) {
    gauge(
        "${METRIC_PREFIX}_antall_lagrede_perioder",
        Tags.empty(),
        antallLagredePerioderReference
    ) {
        antallLagredePerioderReference.get().toDouble()
    }
}
