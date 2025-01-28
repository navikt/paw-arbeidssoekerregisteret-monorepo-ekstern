package no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils

import io.opentelemetry.api.trace.Span

fun Span.perioderKafkaTrace(records: Number) = kafkaTrace("perioder", records)
fun Span.opplysningerKafkaTrace(records: Number) = kafkaTrace("opplysninger", records)
fun Span.profileringerKafkaTrace(records: Number) = kafkaTrace("profileringer", records)
fun Span.bekreftelserKafkaTrace(records: Number) = kafkaTrace("bekreftelser", records)

fun Span.kafkaTrace(type: String, records: Number) =
    traceAttributes(type, "kafka", "database", "write", records)

fun Span.traceAttributes(
    type: String,
    source: String,
    target: String,
    action: String,
    records: Number
) {
    setAttribute("record.type", type)
    setAttribute("source", source)
    setAttribute("target", target)
    setAttribute("action", action)
    setAttribute("record.count", records.toString())
}
