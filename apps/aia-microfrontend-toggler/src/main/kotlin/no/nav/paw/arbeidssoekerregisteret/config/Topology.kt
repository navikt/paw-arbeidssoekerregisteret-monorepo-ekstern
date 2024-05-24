package no.nav.paw.arbeidssoekerregisteret.config

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.config.kafka.streams.KafkaStreamsFactory
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler
import org.apache.kafka.streams.state.Stores

context(ConfigContext, LoggingContext)
fun buildKafkaStreams(topology: Topology): KafkaStreams {
    val streamsFactory = KafkaStreamsFactory(APPLICATION_ID_SUFFIX, kafkaConfig)
        .withDefaultKeySerde(Serdes.Long()::class)
        .withDefaultValueSerde(SpecificAvroSerde::class)

    val kafkaStreams = KafkaStreams(topology, StreamsConfig(streamsFactory.properties))
    kafkaStreams.setUncaughtExceptionHandler { throwable ->
        logger.error("Uventet feil", throwable)
        StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_APPLICATION
    }
    return kafkaStreams
}

context(ConfigContext, LoggingContext)
fun StreamsBuilder.buildTopology(
    meterRegistry: PrometheusMeterRegistry,
    kafkaKeyFunction: (String) -> KafkaKeysResponse
): Topology {
    addPersistentStore(appConfig.kafkaTopology.toggleStoreName, Serdes.Long(), ToggleStateSerde())
    processPeriodeTopic(kafkaKeyFunction)
    return this.build()

}

private fun <K, V> StreamsBuilder.addPersistentStore(
    name: String,
    keySerde: Serde<K>,
    valueSerde: Serde<V>
): StreamsBuilder {
    return this.addStateStore(
        Stores.keyValueStoreBuilder(
            Stores.persistentKeyValueStore(name),
            keySerde,
            valueSerde
        )
    )
}
