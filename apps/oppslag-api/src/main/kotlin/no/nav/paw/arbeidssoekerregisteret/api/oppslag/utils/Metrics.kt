package no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import java.util.concurrent.atomic.AtomicLong

fun MeterRegistry.antallAktivePerioderGauge(antall: AtomicLong) {
    databaseGauge(antall = antall, type = "perioder.aktive")
}

fun MeterRegistry.databaseGauge(antall: AtomicLong, type: String) {
    genericGauge(
        name = "paw.gauge.database.records",
        number = antall,
        type = type,
        source = "database",
        target = "metrics",
        action = "read"
    )
}

fun MeterRegistry.perioderKafkaCounter(antall: Number) {
    kafkaCounter(antall, "perioder")
}

fun MeterRegistry.opplysningerKafkaCounter(antall: Number) {
    kafkaCounter(antall, "opplysninger")
}

fun MeterRegistry.profileringerKafkaCounter(antall: Number) {
    kafkaCounter(antall, "profileringer")
}

fun MeterRegistry.egenvurderingerKafkaCounter(antall: Number) {
    kafkaCounter(antall, "egenvurderinger")
}

fun MeterRegistry.bekreftelserKafkaCounter(antall: Number) {
    kafkaCounter(antall, "bekreftelser")
}

fun MeterRegistry.kafkaCounter(antall: Number, type: String) {
    genericCounter(
        name = "paw.counter.kafka.consumer.messages",
        number = antall,
        type = type,
        source = "kafka",
        target = "database",
        action = "write"
    )
}

fun MeterRegistry.genericCounter(
    name: String,
    number: Number = java.lang.Double.valueOf(1.0),
    type: String,
    source: String,
    target: String,
    action: String
) {
    counter(
        name,
        Tags.of(
            Tag.of("type", type),
            Tag.of("source", source),
            Tag.of("target", target),
            Tag.of("action", action)
        )
    ).increment(number.toDouble())
}

fun MeterRegistry.genericGauge(
    name: String,
    number: Number,
    type: String,
    source: String,
    target: String,
    action: String
) {
    gauge(
        name,
        Tags.of(
            Tag.of("type", type),
            Tag.of("source", source),
            Tag.of("target", target),
            Tag.of("action", action)
        ),
        number
    )
}
