package no.nav.paw.arbeidssoekerregisteret.topology

import io.micrometer.core.instrument.MeterRegistry
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.topology.store.addDeprekeringStateStore
import no.nav.paw.arbeidssoekerregisteret.topology.store.addPeriodeStateStore
import no.nav.paw.arbeidssoekerregisteret.topology.streams.addBeriket14aVedtakStream
import no.nav.paw.arbeidssoekerregisteret.topology.streams.addPeriodeStream
import no.nav.paw.kafkakeygenerator.model.KafkaKeysResponse
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology

fun buildPeriodeTopology(
    applicationConfig: ApplicationConfig,
    meterRegistry: MeterRegistry,
    kafkaKeysFunction: (ident: String) -> KafkaKeysResponse
): Topology = StreamsBuilder().apply {
    addDeprekeringStateStore(applicationConfig)
    addPeriodeStateStore(applicationConfig)
    addPeriodeStream(applicationConfig, meterRegistry, kafkaKeysFunction)
    addBeriket14aVedtakStream(applicationConfig, meterRegistry)
}.build()
