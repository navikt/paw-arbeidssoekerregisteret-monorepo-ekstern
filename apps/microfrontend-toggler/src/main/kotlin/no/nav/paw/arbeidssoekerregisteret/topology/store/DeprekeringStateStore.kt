package no.nav.paw.arbeidssoekerregisteret.topology.store

import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.logging.logger.buildApplicationLogger
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.state.Stores

private val logger = buildApplicationLogger

fun StreamsBuilder.addDeprekeringStateStore(applicationConfig: ApplicationConfig) {
    logger.info("Oppretter State Store {}", applicationConfig.deprekering.stateStore)
    this.addStateStore(
        Stores.keyValueStoreBuilder(
            Stores.persistentKeyValueStore(applicationConfig.deprekering.stateStore),
            Serdes.String(),
            Serdes.String()
        )
    )
}