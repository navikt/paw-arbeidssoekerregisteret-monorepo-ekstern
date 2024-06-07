package no.nav.paw.arbeidssoekerregisteret.plugins


import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.config.HealthIndicator
import no.nav.paw.arbeidssoekerregisteret.config.buildStateListener
import no.nav.paw.arbeidssoekerregisteret.config.buildUncaughtExceptionHandler
import no.nav.paw.arbeidssoekerregisteret.context.ConfigContext
import no.nav.paw.arbeidssoekerregisteret.context.LoggingContext
import no.nav.paw.arbeidssoekerregisteret.plugins.kafka.KafkaStreamsPlugin
import no.nav.paw.arbeidssoekerregisteret.topology.buildTopology
import no.nav.paw.config.kafka.streams.KafkaStreamsFactory
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsConfig

context(ConfigContext, LoggingContext)
fun Application.configureKafka(
    livenessHealthIndicator: HealthIndicator,
    readinessHealthIndicator: HealthIndicator,
    meterRegistry: PrometheusMeterRegistry,
    hentKafkaKeys: (ident: String) -> KafkaKeysResponse?
): KafkaStreams? {
    logger.info("Kafka Streams er enabled for miljø ${appConfig.featureToggles.enableKafkaStreams}")
    if (appConfig.featureToggles.isKafkaStreamsEnabled(appConfig.naisEnv)) {
        val streamsFactory = KafkaStreamsFactory(appConfig.kafkaStreams.applicationIdSuffix, appConfig.kafka)
            .withDefaultKeySerde(Serdes.Long()::class)
            .withDefaultValueSerde(SpecificAvroSerde::class)

        val kafkaStreamsInstance = KafkaStreams(
            buildTopology(meterRegistry, hentKafkaKeys),
            StreamsConfig(streamsFactory.properties)
        )
        kafkaStreamsInstance.setStateListener(buildStateListener(livenessHealthIndicator, readinessHealthIndicator))
        kafkaStreamsInstance.setUncaughtExceptionHandler(buildUncaughtExceptionHandler())

        install(KafkaStreamsPlugin) {
            kafkaStreams = kafkaStreamsInstance
        }

        return kafkaStreamsInstance
    } else {
        livenessHealthIndicator.setHealthy()
        readinessHealthIndicator.setHealthy()
        return null
    }
}

