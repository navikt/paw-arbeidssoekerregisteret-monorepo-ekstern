package no.nav.paw.arbeidssoekerregisteret.plugins


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
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient

context(ConfigContext, LoggingContext)
fun Application.configureKafka(
    healthIndicator: HealthIndicator,
    meterRegistry: PrometheusMeterRegistry,
    kafkaKeysClient: KafkaKeysClient
) {
    install(KafkaStreamsPlugin) {
        kafkaConfig = appConfig.kafka
        kafkaStreamsConfig = appConfig.kafkaStreams
        topology = buildTopology(meterRegistry, kafkaKeysClient)
        stateListener = buildStateListener(healthIndicator)
        exceptionHandler = buildUncaughtExceptionHandler()
    }
}
