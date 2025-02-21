package no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.TraceFlags
import io.opentelemetry.api.trace.TraceState
import io.opentelemetry.context.Context
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory

private val spanHandlerLogger = LoggerFactory.getLogger("spanHandler")
fun initSpan(
    traceparent: TraceParent?,
    instrumentationScopeName: String,
    spanName: String
): ClosableSpan {
    spanHandlerLogger.trace("traceparent: {}", traceparent)
    return traceparent?.value?.split("-")
        ?.takeIf { it.size == 4 }
        ?.let { asArray ->
            SpanContext.createFromRemoteParent(
                asArray[1],
                asArray[2],
                TraceFlags.getSampled(),
                TraceState.getDefault()
            )
        }?.let { spanContext ->
            val spanNoop = Span.wrap(spanContext)
            val originalSPan = Span.current()
            originalSPan.addLink(spanContext)
            val telemetry = GlobalOpenTelemetry.get()
            val tracer = telemetry.tracerProvider.get(instrumentationScopeName)
            tracer.spanBuilder(spanName)
                .setParent(Context.current().with(spanNoop))
                .startSpan()
                .also { it.makeCurrent() }
                .let { ClosableSpan(it, originalSPan) }
        } ?: ClosableSpan(null, null)
}

class ClosableSpan(span: Span?, private val replacedSpan: Span?) : AutoCloseable, Span by (span ?: Span.getInvalid()) {
    fun linkWithReplacedSpan(): ClosableSpan {
        replacedSpan?.also { it.addLink(Span.current().spanContext) }
        return this
    }

    override fun close() {
        end()
        replacedSpan?.makeCurrent()
    }
}

fun ConsumerRecord<*, *>.traceparent(): TraceParent? {
    return headers().lastHeader("traceparent")?.let { TraceParent(String(it.value())) }
}

@JvmInline
value class TraceParent(val value: String)

val invalidTraceParent = TraceParent(Span.getInvalid().spanContext.let{ctx -> "00-${ctx.traceId}-${ctx.spanId}-00"})