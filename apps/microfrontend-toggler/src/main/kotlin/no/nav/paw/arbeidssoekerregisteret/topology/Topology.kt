package no.nav.paw.arbeidssoekerregisteret.topology

import io.micrometer.core.instrument.MeterRegistry
import no.nav.paw.arbeidssoekerregisteret.config.buildPeriodeInfoSerde
import no.nav.paw.arbeidssoekerregisteret.context.ConfigContext
import no.nav.paw.arbeidssoekerregisteret.context.LoggingContext
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.state.Stores

context(ConfigContext, LoggingContext)
fun buildTopology(
    meterRegistry: MeterRegistry,
    hentKafkaKeys: (ident: String) -> KafkaKeysResponse?
): Topology = StreamsBuilder().apply {
    addPeriodeStateStore()
    logger.info(
        "Kafka Streams Periode Topology er enabled for miljø {}",
        appConfig.featureToggles.enablePeriodeTopology
    )
    if (appConfig.featureToggles.isPeriodeTopologyEnabled(appConfig.naisEnv)) {
        buildPeriodeTopology(meterRegistry, hentKafkaKeys)
    }
    logger.info(
        "Kafka Streams 14s Vedtak Topology er enabled for miljø {}",
        appConfig.featureToggles.enable14aVedtakTopology
    )
    if (appConfig.featureToggles.is14aVedtakTopologyEnabled(appConfig.naisEnv)) {
        buildSiste14aVedtakTopology(meterRegistry, hentKafkaKeys)
    }
}.build()

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
