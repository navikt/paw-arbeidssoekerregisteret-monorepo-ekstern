package no.nav.paw.arbeidssoekerregisteret.config

import io.micrometer.core.instrument.Tag
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.model.Toggle

private const val METRIC_PREFIX = "paw_microfrontend_toggler"

fun PrometheusMeterRegistry.tellAntallToggles(toggle: Toggle) {
    counter(
        "${METRIC_PREFIX}_processed_actions",
        listOf(
            Tag.of("microfrontend_id", toggle.microfrontendId),
            Tag.of("toggle_action", toggle.action.value)
        )
    ).increment()
}
