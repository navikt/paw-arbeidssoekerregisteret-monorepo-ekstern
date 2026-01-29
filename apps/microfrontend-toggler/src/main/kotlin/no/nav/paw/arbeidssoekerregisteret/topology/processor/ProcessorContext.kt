package no.nav.paw.arbeidssoekerregisteret.topology.processor

import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssoekerregisteret.model.PeriodeInfo
import no.nav.paw.arbeidssoekerregisteret.model.Sensitivitet
import no.nav.paw.arbeidssoekerregisteret.model.Toggle
import no.nav.paw.arbeidssoekerregisteret.model.ToggleAction
import no.nav.paw.arbeidssoekerregisteret.model.ToggleSource
import org.apache.kafka.streams.processor.api.ProcessorContext

@WithSpan(value = "microfrontend_toggle", kind = SpanKind.INTERNAL)
fun ProcessorContext<Long, Toggle>.iverksettAktiverToggle(
    periodeInfo: PeriodeInfo,
    microfrontendId: String,
    toggleSource: ToggleSource,
    sensitivitet: Sensitivitet
): Toggle {
    val currentSpan = Span.current()
    currentSpan.setAttribute("action", ToggleAction.ENABLE.value)
    currentSpan.setAttribute("target", microfrontendId)
    currentSpan.setAttribute("source", toggleSource.value)
    val enableToggle = periodeInfo.asEnableToggle(microfrontendId, sensitivitet)
    forward(enableToggle.asRecord(periodeInfo.arbeidssoekerId))
    return enableToggle
}

@WithSpan(value = "microfrontend_toggle", kind = SpanKind.INTERNAL)
fun ProcessorContext<Long, Toggle>.iverksettDeaktiverToggle(
    periodeInfo: PeriodeInfo,
    microfrontendId: String,
    toggleSource: ToggleSource
): Toggle {
    val currentSpan = Span.current()
    currentSpan.setAttribute("action", ToggleAction.DISABLE.value)
    currentSpan.setAttribute("target", microfrontendId)
    currentSpan.setAttribute("source", toggleSource.value)
    val disableToggle = periodeInfo.asDisableToggle(microfrontendId)
    forward(disableToggle.asRecord(periodeInfo.arbeidssoekerId))
    return disableToggle
}