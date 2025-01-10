package no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags

fun MeterRegistry.antallAktivePerioder(antall: Number) {
    gauge(
        "paw_arbeidssoekerregisteret_eksternt_api_antall_aktive_perioder",
        Tags.empty(),
        antall
    )
}
