package no.nav.paw.arbeidssoekerregisteret.topology

import io.micrometer.core.instrument.MeterRegistry
import no.nav.paw.arbeidssoekerregisteret.config.AppConfig
import no.nav.paw.arbeidssoekerregisteret.config.buildPeriodeInfoSerde
import no.nav.paw.arbeidssoekerregisteret.topology.streams.buildBeriket14aVedtakKStream
import no.nav.paw.arbeidssoekerregisteret.topology.streams.buildPeriodeKStream
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.state.Stores

private fun StreamsBuilder.addPeriodeStateStore(appConfig: AppConfig) {
    this.addStateStore(
        Stores.keyValueStoreBuilder(
            Stores.persistentKeyValueStore(appConfig.kafkaStreams.periodeStoreName),
            Serdes.Long(),
            buildPeriodeInfoSerde()
        )
    )
}

fun buildPeriodeTopology(
    appConfig: AppConfig,
    meterRegistry: MeterRegistry,
    hentKafkaKeys: (ident: String) -> KafkaKeysResponse?
): Topology = StreamsBuilder().apply {
    addPeriodeStateStore(appConfig)
    buildPeriodeKStream(appConfig, meterRegistry, hentKafkaKeys)
    buildBeriket14aVedtakKStream(appConfig, meterRegistry)
}.build()
