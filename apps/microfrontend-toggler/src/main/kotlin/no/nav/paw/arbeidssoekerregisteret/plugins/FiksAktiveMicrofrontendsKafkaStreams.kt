package no.nav.paw.arbeidssoekerregisteret.plugins


import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import io.ktor.server.application.Application
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.config.buildUncaughtExceptionHandler
import no.nav.paw.arbeidssoekerregisteret.context.ConfigContext
import no.nav.paw.arbeidssoekerregisteret.context.LoggingContext
import no.nav.paw.arbeidssoekerregisteret.topology.addFiksAktiveMicrofrontendsStateStore
import no.nav.paw.arbeidssoekerregisteret.topology.addFiksAktiveMicrofrontendsStream
import no.nav.paw.config.kafka.streams.KafkaStreamsFactory
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig

private const val KAFKA_STREAMS_SUFFIX = "fiks-aktive-microfrontends-stream-v1"

context(ConfigContext, LoggingContext)
fun Application.configureFiksAktiveMicrofrontendsKafkaStreams(
    meterRegistry: PrometheusMeterRegistry,
    hentKafkaKeys: (ident: String) -> KafkaKeysResponse?
) {
    logger.info("Kafka Streams for å fikse aktive microfrontends er enabled for miljø ${appConfig.featureToggles.enableKafkaStreams}")
    if (appConfig.featureToggles.isFiksAktiveMicrofrontendsKafkaStreamsEnabled(appConfig.naisEnv)) {
        val streamsFactory =
            KafkaStreamsFactory(KAFKA_STREAMS_SUFFIX, appConfig.kafka).withDefaultKeySerde(Serdes.Long()::class)
                .withDefaultValueSerde(SpecificAvroSerde::class)

        val kafkaStreams = KafkaStreams(
            StreamsBuilder().apply {
                addFiksAktiveMicrofrontendsStateStore()
                addFiksAktiveMicrofrontendsStream(meterRegistry, hentKafkaKeys)
            }.build(),
            StreamsConfig(streamsFactory.properties)
        )
        kafkaStreams.setUncaughtExceptionHandler(buildUncaughtExceptionHandler())
        kafkaStreams.start()
    }
}