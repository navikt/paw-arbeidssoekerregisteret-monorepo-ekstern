package no.nav.paw.arbeidssoekerregisteret.topology

import io.micrometer.core.instrument.MeterRegistry
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.topology.streams.buildBeriket14aVedtakStream
import no.nav.paw.arbeidssoekerregisteret.topology.streams.buildPeriodeStream
import no.nav.paw.arbeidssoekerregisteret.utils.buildPeriodeInfoSerde
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.state.Stores

private fun StreamsBuilder.addPeriodeStateStore(applicationConfig: ApplicationConfig) {
    this.addStateStore(
        Stores.keyValueStoreBuilder(
            Stores.persistentKeyValueStore(applicationConfig.kafkaStreams.periodeStoreName),
            Serdes.Long(),
            buildPeriodeInfoSerde()
        )
    )
}

fun buildPeriodeTopology(
    applicationConfig: ApplicationConfig,
    meterRegistry: MeterRegistry,
    kafkaKeysFunction: (ident: String) -> KafkaKeysResponse
): Topology = StreamsBuilder().apply {
    addPeriodeStateStore(applicationConfig)
    buildPeriodeStream(applicationConfig, meterRegistry, kafkaKeysFunction)
    buildBeriket14aVedtakStream(applicationConfig, meterRegistry)
}.build()
