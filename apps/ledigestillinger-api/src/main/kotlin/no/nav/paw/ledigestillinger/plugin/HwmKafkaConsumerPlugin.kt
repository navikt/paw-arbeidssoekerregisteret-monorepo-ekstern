package no.nav.paw.ledigestillinger.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationPlugin
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.MonitoringEvent
import io.ktor.server.application.install
import io.ktor.server.application.log
import no.nav.paw.hwm.DataConsumer
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("no.nav.paw.logger.kafka.consumer")

fun Application.installKafkaConsumerPlugin(
    kafkaConsumer: DataConsumer<*, *, *>
) {
    install(KafkaConsumerPlugin) {
        this.kafkaConsumer = kafkaConsumer
    }
}

class KafkaConsumerPluginConfig {
    var kafkaConsumer: DataConsumer<*, *, *>? = null
}

val KafkaConsumerPlugin: ApplicationPlugin<KafkaConsumerPluginConfig> =
    createApplicationPlugin("KafkaConsumerPlugin", ::KafkaConsumerPluginConfig) {
        application.log.info("Installerer KafkaConsumerPlugin")
        val kafkaConsumer = requireNotNull(pluginConfig.kafkaConsumer) { "KafkaConsumer er null" }

        on(MonitoringEvent(ApplicationStarted)) {
            logger.info("Klargjør Kafka Consumer")
            kafkaConsumer.runAndCloseOnExit()
                .handle { _, throwable ->
                    if (throwable != null) {
                        logger.error("Kafka Consumer stoppet grunnet feil", throwable)
                    } else {
                        logger.info("Kafka Consumer stoppet")
                    }
                }
        }

        on(MonitoringEvent(ApplicationStopping)) {
            logger.info("Stopper Kafka Consumer")
            kafkaConsumer.close()
        }
    }
