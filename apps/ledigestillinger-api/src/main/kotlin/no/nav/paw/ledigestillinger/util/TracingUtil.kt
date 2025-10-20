package no.nav.paw.ledigestillinger.util

import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span

fun Span.meldingerMottattEvent(
    antallTotal: Number,
    antallProsessert: Number,
    antallLagret: Number
): Span = apply {
    addEvent(
        "meldinger_mottatt",
        Attributes.of(
            stringKey("antall_meldinger_totalt"), antallTotal.toString(),
            stringKey("antall_meldinger_prosessert"), antallProsessert.toString(),
            stringKey("antall_meldinger_lagret"), antallLagret.toString(),
        )
    )
}