package no.nav.paw.arbeidssoekerregisteret.plugins.kafka

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import io.ktor.server.application.ApplicationPlugin
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.MonitoringEvent
import io.ktor.server.application.log
import io.ktor.util.KtorDsl
import no.nav.paw.arbeidssoekerregisteret.config.APPLICATION_ID_SUFFIX
import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.config.kafka.streams.KafkaStreamsFactory
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler
import java.time.Duration

@KtorDsl
class KafkaStreamsPluginConfig {
    var config: KafkaConfig? = null
    var topology: Topology? = null
    var exceptionHandler: StreamsUncaughtExceptionHandler? = null
}

val KafkaStreamsPlugin: ApplicationPlugin<KafkaStreamsPluginConfig> =
    createApplicationPlugin("KafkaStreams", ::KafkaStreamsPluginConfig) {
        val config = requireNotNull(pluginConfig.config) { "KafkaConfig er null" }
        val topology = requireNotNull(pluginConfig.topology) { "Topology er null" }
        val exceptionHandler = requireNotNull(pluginConfig.exceptionHandler) { "ExceptionHandler er null" }

        val streamsFactory = KafkaStreamsFactory(APPLICATION_ID_SUFFIX, config)
            .withDefaultKeySerde(Serdes.Long()::class)
            .withDefaultValueSerde(SpecificAvroSerde::class)

        val kafkaStreams = KafkaStreams(topology, StreamsConfig(streamsFactory.properties))
        kafkaStreams.setUncaughtExceptionHandler(exceptionHandler)

        on(MonitoringEvent(ApplicationStarted)) { application ->
            application.log.info("Starting Kafka Streams")
            kafkaStreams.start()
        }

        on(MonitoringEvent(ApplicationStopping)) { application ->
            application.log.info("Stopping Kafka Streams")
            kafkaStreams.close(Duration.ofSeconds(5))
        }
    }