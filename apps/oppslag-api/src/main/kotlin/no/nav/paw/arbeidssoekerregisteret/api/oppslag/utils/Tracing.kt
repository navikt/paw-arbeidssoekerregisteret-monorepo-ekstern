package no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils

import io.opentelemetry.api.trace.Span

fun Span.perioderKafkaTrace() = kafkaTrace("perioder")
fun Span.opplysningerKafkaTrace() = kafkaTrace("opplysninger")
fun Span.profileringerKafkaTrace() = kafkaTrace("profileringer")
fun Span.bekreftelserKafkaTrace() = kafkaTrace("bekreftelser")

fun Span.kafkaTrace(type: String) = traceAttributes(type, "kafka", "database", "write")

fun Span.traceAttributes(
    type: String,
    source: String,
    target: String,
    action: String
) {
    setAttribute("type", type)
    setAttribute("source", source)
    setAttribute("target", target)
    setAttribute("action", action)
}
