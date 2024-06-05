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
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import no.nav.paw.pdl.graphql.generated.hentidenter.IdentInformasjon

context(ConfigContext, LoggingContext)
fun Application.configureKafka(
    healthIndicator: HealthIndicator,
    meterRegistry: PrometheusMeterRegistry,
    hentKafkaKeys: (ident: String) -> KafkaKeysResponse?,
    hentFolkeregisterIdent: (aktorId: String) -> IdentInformasjon?
) {
    logger.info("Kafka Streams er enabled for miljø ${appConfig.kafkaStreams.enabledForEnvs}")
    if (appConfig.kafkaStreams.enabledForEnvs.contains(appConfig.naisEnv.clusterName)) {
        install(KafkaStreamsPlugin) {
            kafkaConfig = appConfig.kafka
            kafkaStreamsConfig = appConfig.kafkaStreams
            topology = buildTopology(meterRegistry, hentKafkaKeys, hentFolkeregisterIdent)
            stateListener = buildStateListener(healthIndicator)
            exceptionHandler = buildUncaughtExceptionHandler()
        }
    } else {
        healthIndicator.setHealthy()
    }
}
