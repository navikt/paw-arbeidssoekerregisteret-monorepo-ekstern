package no.nav.paw.arbeidssoekerregisteret.plugins.kafka

import io.ktor.server.application.ApplicationPlugin
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.MonitoringEvent
import io.ktor.server.application.log
import io.ktor.util.KtorDsl
import no.nav.paw.arbeidssoekerregisteret.config.KafkaStreamsConfig
import org.apache.kafka.streams.KafkaStreams

@KtorDsl
class KafkaStreamsPluginConfig {
    var kafkaStreamsConfig: KafkaStreamsConfig? = null
    var kafkaStreams: KafkaStreams? = null
}

val KafkaStreamsPlugin: ApplicationPlugin<KafkaStreamsPluginConfig> =
    createApplicationPlugin("KafkaStreams", ::KafkaStreamsPluginConfig) {
        val kafkaStreamsConfig = requireNotNull(pluginConfig.kafkaStreamsConfig) { "KafkaStreamsConfig er null" }
        val kafkaStreams = requireNotNull(pluginConfig.kafkaStreams) { "KafkaStreams er null" }

        on(MonitoringEvent(ApplicationStarted)) { application ->
            application.log.info("Starting Kafka Streams")
            kafkaStreams.start()
        }

        on(MonitoringEvent(ApplicationStopping)) { application ->
            application.log.info("Stopping Kafka Streams")
            kafkaStreams.close(kafkaStreamsConfig.shutDownTimeout)
        }
    }