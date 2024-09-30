package no.nav.paw.arbeidssoekerregisteret.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.opentelemetry.api.trace.Span

val OpenTelemetryPlugin = createApplicationPlugin("OpenTelemetryPlugin") {
    onCallRespond { call, _ ->
        runCatching { Span.current().spanContext.traceId }
            .onSuccess { call.response.headers.append("x-trace-id", it) }
    }
}

fun Application.configureTracing() {
    install(OpenTelemetryPlugin)
}