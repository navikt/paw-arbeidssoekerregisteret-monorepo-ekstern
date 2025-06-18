package no.nav.paw.arbeidssoekerregisteret.topology

import io.micrometer.core.instrument.MeterRegistry
import no.nav.paw.arbeidssoekerregisteret.context.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.topology.streams.buildProfileringStream
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.state.Stores

private fun StreamsBuilder.addProfileringStateStore(applicationContext: ApplicationContext) {
    this.addStateStore(
        Stores.keyValueStoreBuilder(
            Stores.persistentKeyValueStore(applicationContext.applicationConfig.kafkaTopology.profileringStateStoreName),
            Serdes.Long(),
            applicationContext.profileringSerde

        )
    )
}

fun buildPeriodeTopology(
    applicationContext: ApplicationContext,
    meterRegistry: MeterRegistry,
    kafkaKeysFunction: (ident: String) -> KafkaKeysResponse
): Topology = StreamsBuilder().apply {
    addProfileringStateStore(applicationContext)
    buildProfileringStream(applicationContext.applicationConfig, applicationContext.profileringSerde)
}.build()
