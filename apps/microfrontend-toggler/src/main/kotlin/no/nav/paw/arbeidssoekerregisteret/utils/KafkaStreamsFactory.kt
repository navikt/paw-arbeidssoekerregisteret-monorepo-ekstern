package no.nav.paw.arbeidssoekerregisteret.utils

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import no.nav.paw.error.handler.withApplicationTerminatingExceptionHandler
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.factory.KafkaStreamsFactory
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.Topology

fun buildKafkaStreams(
    applicationIdSuffix: String,
    kafkaConfig: KafkaConfig,
    topology: Topology,
): KafkaStreams {
    val streamsFactory = KafkaStreamsFactory(applicationIdSuffix, kafkaConfig)
        .withDefaultKeySerde(Serdes.Long()::class)
        .withDefaultValueSerde(SpecificAvroSerde::class)

    val kafkaStreams = KafkaStreams(topology, StreamsConfig(streamsFactory.properties))
    kafkaStreams.withApplicationTerminatingExceptionHandler()
    return kafkaStreams
}
