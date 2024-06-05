package no.nav.paw.arbeidssoekerregisteret.config

import io.micrometer.core.instrument.Tag
import io.micrometer.prometheus.PrometheusMeterRegistry


fun PrometheusMeterRegistry.tellIkkeIPDL() {
    counter(
        "paw_microfrontend_toggler_filter",
        listOf(Tag.of("resultat", "not_in_pdl"))
    ).increment()
}
