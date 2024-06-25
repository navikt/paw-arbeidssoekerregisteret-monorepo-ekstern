package no.nav.paw.arbeidssoekerregisteret.topology

import io.micrometer.core.instrument.MeterRegistry
import no.nav.paw.arbeidssoekerregisteret.config.buildPeriodeInfoSerde
import no.nav.paw.arbeidssoekerregisteret.context.ConfigContext
import no.nav.paw.arbeidssoekerregisteret.context.LoggingContext
import no.nav.paw.arbeidssoekerregisteret.topology.streams.buildBeriket14aVedtakKStream
import no.nav.paw.arbeidssoekerregisteret.topology.streams.buildPeriodeKStream
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.state.Stores

context(ConfigContext)
private fun StreamsBuilder.addPeriodeStateStore() {
    this.addStateStore(
        Stores.keyValueStoreBuilder(
            Stores.persistentKeyValueStore(appConfig.kafkaStreams.periodeStoreName),
            Serdes.Long(),
            buildPeriodeInfoSerde()
        )
    )
}

context(ConfigContext, LoggingContext)
fun buildPeriodeTopology(
    meterRegistry: MeterRegistry,
    hentKafkaKeys: (ident: String) -> KafkaKeysResponse?
): Topology = StreamsBuilder().apply {
    addPeriodeStateStore()
    buildPeriodeKStream(meterRegistry, hentKafkaKeys)
    buildBeriket14aVedtakKStream(meterRegistry, hentKafkaKeys)
}.build()
