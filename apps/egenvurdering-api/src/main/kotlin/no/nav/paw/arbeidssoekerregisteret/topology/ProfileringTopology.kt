package no.nav.paw.arbeidssoekerregisteret.topology

import io.micrometer.core.instrument.MeterRegistry
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.topology.streams.buildProfileringStream
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.state.Stores

private fun StreamsBuilder.addProfileringStateStore(applicationConfig: ApplicationConfig, profileringSerde: Serde<Profilering>) {
    this.addStateStore(
        Stores.keyValueStoreBuilder(
            Stores.persistentKeyValueStore(applicationConfig.kafkaTopology.profileringStateStoreName),
            Serdes.Long(),
            profileringSerde
        )
    )
}

fun buildProfileringTopology(
    applicationConfig: ApplicationConfig,
    profileringSerde: Serde<Profilering>,
    meterRegistry: MeterRegistry,
    kafkaKeysFunction: (ident: String) -> KafkaKeysResponse
): Topology = StreamsBuilder().apply {
    addProfileringStateStore(applicationConfig, profileringSerde)
    buildProfileringStream(applicationConfig, profileringSerde)
}.build()
