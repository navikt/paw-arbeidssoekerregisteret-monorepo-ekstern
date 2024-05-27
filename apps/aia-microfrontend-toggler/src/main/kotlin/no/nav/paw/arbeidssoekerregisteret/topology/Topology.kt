package no.nav.paw.arbeidssoekerregisteret.topology

import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.config.buildToggleStateSerde
import no.nav.paw.arbeidssoekerregisteret.context.ConfigContext
import no.nav.paw.arbeidssoekerregisteret.context.LoggingContext
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.state.Stores

context(ConfigContext, LoggingContext)
fun buildTopology(
    meterRegistry: PrometheusMeterRegistry,
    kafkaKeyFunction: (String) -> KafkaKeysResponse
): Topology = StreamsBuilder().apply {
    addTogglePersistentStore()
    buildPeriodeTopology(kafkaKeyFunction)
}.build()

context(ConfigContext)
private fun StreamsBuilder.addTogglePersistentStore() {
    this.addStateStore(
        Stores.keyValueStoreBuilder(
            Stores.persistentKeyValueStore(appConfig.kafkaTopology.toggleStoreName),
            Serdes.Long(),
            buildToggleStateSerde()
        )
    )
}
