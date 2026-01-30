package no.nav.paw.arbeidssoekerregisteret.topology.store

import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.utils.buildPeriodeInfoSerde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.state.Stores

fun StreamsBuilder.addPeriodeStateStore(applicationConfig: ApplicationConfig) {
    this.addStateStore(
        Stores.keyValueStoreBuilder(
            Stores.persistentKeyValueStore(applicationConfig.kafkaTopology.periodeStoreName),
            Serdes.Long(),
            buildPeriodeInfoSerde()
        )
    )
}