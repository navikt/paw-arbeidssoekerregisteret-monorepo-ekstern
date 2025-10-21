package no.nav.paw.ledigestillinger.util

import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span

fun Span.meldingerMottattEvent(
    antallMottatt: Number,
    antallLagret: Number,
    millisekunder: Number
): Span = apply {
    addEvent(
        "meldinger_mottatt",
        Attributes.of(
            stringKey("mottatt"), antallMottatt.toString(),
            stringKey("lagret"), antallLagret.toString(),
            stringKey("millisekunder"), millisekunder.toString(),
        )
    )
}