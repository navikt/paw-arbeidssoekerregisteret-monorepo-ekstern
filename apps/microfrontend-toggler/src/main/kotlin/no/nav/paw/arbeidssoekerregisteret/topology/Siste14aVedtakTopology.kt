package no.nav.paw.arbeidssoekerregisteret.topology

import io.micrometer.core.instrument.MeterRegistry
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.topology.streams.addSiste14aVedtakStream
import no.nav.paw.kafkakeygenerator.model.KafkaKeysResponse
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology

fun buildSiste14aVedtakTopology(
    applicationConfig: ApplicationConfig,
    meterRegistry: MeterRegistry,
    kafkaKeysFunction: (ident: String) -> KafkaKeysResponse?
): Topology = StreamsBuilder().apply {
    addSiste14aVedtakStream(applicationConfig, meterRegistry, kafkaKeysFunction)
}.build()
