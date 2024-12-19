package no.nav.paw.arbeidssoekerregisteret.utils

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import no.nav.paw.error.handler.withApplicationTerminatingExceptionHandler
import no.nav.paw.health.listener.withHealthIndicatorStateListener
import no.nav.paw.health.model.LivenessHealthIndicator
import no.nav.paw.health.model.ReadinessHealthIndicator
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.factory.KafkaStreamsFactory
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.Topology

fun buildKafkaStreams(
    applicationIdSuffix: String,
    kafkaConfig: KafkaConfig,
    healthIndicatorRepository: HealthIndicatorRepository,
    topology: Topology
): KafkaStreams {
    val livenessIndicator = healthIndicatorRepository.addLivenessIndicator(LivenessHealthIndicator())
    val readinessIndicator = healthIndicatorRepository.addReadinessIndicator(ReadinessHealthIndicator())

    val streamsFactory = KafkaStreamsFactory(applicationIdSuffix, kafkaConfig)
        .withDefaultKeySerde(Serdes.Long()::class)
        .withDefaultValueSerde(SpecificAvroSerde::class)

    val kafkaStreams = KafkaStreams(
        topology,
        StreamsConfig(streamsFactory.properties)
    )
    kafkaStreams.withHealthIndicatorStateListener(livenessIndicator, readinessIndicator)
    kafkaStreams.withApplicationTerminatingExceptionHandler()
    return kafkaStreams
}
