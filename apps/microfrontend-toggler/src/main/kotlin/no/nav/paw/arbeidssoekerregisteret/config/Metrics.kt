package no.nav.paw.arbeidssoekerregisteret.config

import io.micrometer.core.instrument.Tag
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.model.PeriodeInfo
import no.nav.paw.arbeidssoekerregisteret.model.Toggle

fun PrometheusMeterRegistry.tellAntallToggles(periodeInfo: PeriodeInfo, toggle: Toggle) {
    /*counter(
        "paw_microfrontend_toggler_processed",
        listOf(
            Tag.of("periode", periodeInfo.id.toString()),
            Tag.of("microfrontend", toggle.microfrontendId),
            Tag.of("toggle_action", toggle.action.value)
        )
    ).increment()*/
}
