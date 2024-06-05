package no.nav.paw.arbeidssoekerregisteret.topology

import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.config.buildPeriodeInfoSerde
import no.nav.paw.arbeidssoekerregisteret.context.ConfigContext
import no.nav.paw.arbeidssoekerregisteret.context.LoggingContext
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import no.nav.paw.pdl.graphql.generated.hentidenter.IdentInformasjon
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.state.Stores

context(ConfigContext, LoggingContext)
fun buildTopology(
    meterRegistry: PrometheusMeterRegistry,
    hentKafkaKeys: (ident: String) -> KafkaKeysResponse?,
    hentFolkeregisterIdent: (aktorId: String) -> IdentInformasjon?
): Topology = StreamsBuilder().apply {
    addPeriodeStateStore()
    logger.info("Kafka Streams Periode Topology er enabled for miljø ${appConfig.featureToggle.enablePeriodeTopology}")
    if (appConfig.featureToggle.enablePeriodeTopology.contains(appConfig.naisEnv.clusterName)) {
        buildPeriodeTopology(meterRegistry, hentKafkaKeys)
    }
    logger.info("Kafka Streams 14s Vedtak Topology er enabled for miljø ${appConfig.featureToggle.enable14aVedtakTopology}")
    if (appConfig.featureToggle.enable14aVedtakTopology.contains(appConfig.naisEnv.clusterName)) {
        buildSiste14aVedtakTopology(meterRegistry, hentKafkaKeys, hentFolkeregisterIdent)
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
