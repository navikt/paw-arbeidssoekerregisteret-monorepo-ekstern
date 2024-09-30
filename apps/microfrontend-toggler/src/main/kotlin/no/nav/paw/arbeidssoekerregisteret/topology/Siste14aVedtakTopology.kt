package no.nav.paw.arbeidssoekerregisteret.topology

import io.micrometer.core.instrument.MeterRegistry
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.topology.streams.buildSiste14aVedtakStream
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology

fun buildSiste14aVedtakTopology(
    applicationConfig: ApplicationConfig,
    meterRegistry: MeterRegistry,
    kafkaKeysFunction: (ident: String) -> KafkaKeysResponse?
): Topology = StreamsBuilder().apply {
    buildSiste14aVedtakStream(applicationConfig, meterRegistry, kafkaKeysFunction)
}.build()
