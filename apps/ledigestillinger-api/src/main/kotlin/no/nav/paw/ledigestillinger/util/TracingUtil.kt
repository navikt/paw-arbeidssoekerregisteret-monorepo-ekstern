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

fun Span.finnStillingerByUuidListeEvent(
    antallUuider: Number,
): Span = apply {
    addEvent(
        "finn_stillinger_by_egenskaper",
        Attributes.of(
            stringKey("uuidListe"), antallUuider.toString()
        )
    )
}

fun Span.finnStillingerByEgenskaperEvent(
    antallSoekeord: Number,
    antallKategorier: Number,
    antallFylker: Number,
    antallKommuner: Number
): Span = apply {
    addEvent(
        "finn_stillinger_by_egenskaper",
        Attributes.of(
            stringKey("soekeord"), antallSoekeord.toString(),
            stringKey("kategorier"), antallKategorier.toString(),
            stringKey("fylker"), antallFylker.toString(),
            stringKey("kommuner"), antallKommuner.toString(),
        )
    )
}