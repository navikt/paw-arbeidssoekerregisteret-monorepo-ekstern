package no.nav.paw.arbeidssoekerregisteret.plugins


import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.context.ConfigContext
import no.nav.paw.arbeidssoekerregisteret.context.LoggingContext
import no.nav.paw.arbeidssoekerregisteret.plugins.kafka.KafkaStreamsPlugin
import no.nav.paw.arbeidssoekerregisteret.topology.buildTopology
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler

context(ConfigContext, LoggingContext)
fun Application.configureKafka(
    meterRegistry: PrometheusMeterRegistry,
    kafkaKeysClient: KafkaKeysClient
) {
    install(KafkaStreamsPlugin) {
        config = kafkaConfig
        topology = buildTopology(meterRegistry, kafkaKeysClient)
        exceptionHandler = loggingUncaughtExceptionHandler()
    }
}

context(LoggingContext)
fun loggingUncaughtExceptionHandler() = StreamsUncaughtExceptionHandler { throwable ->
    logger.error("Uventet feil", throwable)
    StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_APPLICATION
}