package no.nav.paw.arbeidssoekerregisteret.topology.processor

import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssoekerregisteret.model.MicroFrontend
import no.nav.paw.arbeidssoekerregisteret.model.PeriodeInfo
import no.nav.paw.arbeidssoekerregisteret.model.Sensitivitet
import no.nav.paw.arbeidssoekerregisteret.model.Toggle
import no.nav.paw.arbeidssoekerregisteret.model.ToggleAction
import no.nav.paw.arbeidssoekerregisteret.model.ToggleSource
import org.apache.kafka.streams.processor.api.ProcessorContext

@WithSpan(value = "microfrontend_toggle", kind = SpanKind.INTERNAL)
fun ProcessorContext<Long, Toggle>.iverksettAktiverToggle(
    periodeInfo: PeriodeInfo,
    microfrontend: MicroFrontend,
    toggleSource: ToggleSource,
    sensitivitet: Sensitivitet
): Toggle {
    val currentSpan = Span.current()
    currentSpan.setAttribute("action", ToggleAction.ENABLE.value)
    currentSpan.setAttribute("target", microfrontend.value)
    currentSpan.setAttribute("source", toggleSource.value)
    val enableToggle = periodeInfo.asEnableToggle(microfrontend, sensitivitet)
    forward(enableToggle.asRecord(periodeInfo.arbeidssoekerId))
    return enableToggle
}

@WithSpan(value = "microfrontend_toggle", kind = SpanKind.INTERNAL)
fun ProcessorContext<Long, Toggle>.iverksettDeaktiverToggle(
    periodeInfo: PeriodeInfo,
    microfrontend: MicroFrontend,
    toggleSource: ToggleSource
): Toggle {
    val currentSpan = Span.current()
    currentSpan.setAttribute("action", ToggleAction.DISABLE.value)
    currentSpan.setAttribute("target", microfrontend.value)
    currentSpan.setAttribute("source", toggleSource.value)
    val disableToggle = periodeInfo.asDisableToggle(microfrontend)
    forward(disableToggle.asRecord(periodeInfo.arbeidssoekerId))
    return disableToggle
}