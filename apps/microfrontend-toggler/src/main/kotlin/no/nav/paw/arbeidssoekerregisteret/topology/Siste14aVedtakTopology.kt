package no.nav.paw.arbeidssoekerregisteret.topology

import io.micrometer.core.instrument.MeterRegistry
import no.nav.paw.arbeidssoekerregisteret.context.ConfigContext
import no.nav.paw.arbeidssoekerregisteret.context.LoggingContext
import no.nav.paw.arbeidssoekerregisteret.topology.streams.buildSiste14aVedtakKStream
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology

context(ConfigContext, LoggingContext)
fun buildSiste14aVedtakTopology(
    meterRegistry: MeterRegistry,
    hentKafkaKeys: (ident: String) -> KafkaKeysResponse?
): Topology = StreamsBuilder().apply {
    buildSiste14aVedtakKStream(meterRegistry, hentKafkaKeys)
}.build()
